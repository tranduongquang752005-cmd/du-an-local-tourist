package dao;

import config.DatabaseConnection;
import model.FeaturedTourView;
import model.PopularTourView;
import model.Tour;

import java.math.BigDecimal;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class TourDAO {

    public List<Tour> getAllTours() {
        String sql = buildTourSql("""
                ORDER BY t.TourID ASC
                """);

        return queryTourList(sql, null, "getAllTours");
    }

    public List<Tour> getAllActiveTours() {
        String sql = buildTourSql("""
                WHERE t.IsActive = 1
                ORDER BY t.TourID ASC
                """);

        return queryTourList(sql, null, "getAllActiveTours");
    }

    public List<Tour> getInactiveTours() {
        String sql = buildTourSql("""
                WHERE t.IsActive = 0
                ORDER BY t.TourID ASC
                """);

        return queryTourList(sql, null, "getInactiveTours");
    }

    public List<Tour> getToursPaging(int page, int pageSize) {
        int validPage = Math.max(page, 1);
        int validPageSize = normalizePageSize(pageSize);
        int offset = (validPage - 1) * validPageSize;

        String sql = buildTourSql("""
                WHERE t.IsActive = 1
                ORDER BY t.TourID ASC
                OFFSET ? ROWS FETCH NEXT ? ROWS ONLY
                """);

        return queryTourList(
                sql,
                ps -> {
                    ps.setInt(1, offset);
                    ps.setInt(2, validPageSize);
                },
                "getToursPaging"
        );
    }

    public int countActiveTours() {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOURS
                WHERE IsActive = 1
                """;

        return queryInt(sql, null, "countActiveTours");
    }

    public Tour getTourById(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return null;
        }

        String sql = buildTourSql("""
                WHERE t.TourID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTour(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTourById");
        }

        return null;
    }

    public List<Tour> searchTours(String keyword) {
        String cleanKeyword = cleanString(keyword);

        if (cleanKeyword == null) {
            return getAllActiveTours();
        }

        String sql = buildTourSql("""
                WHERE t.IsActive = 1
                  AND (
                        t.TourName LIKE ?
                     OR t.Description LIKE ?
                     OR t.Theme LIKE ?
                     OR l.LocationName LIKE ?
                     OR c.CategoryName LIKE ?
                  )
                ORDER BY t.TourID ASC
                """);

        return queryTourList(
                sql,
                ps -> {
                    String likeKeyword = "%" + cleanKeyword + "%";
                    for (int i = 1; i <= 5; i++) {
                        ps.setString(i, likeKeyword);
                    }
                },
                "searchTours"
        );
    }

    public List<Tour> searchToursForHome(String locationKeyword,
                                         LocalDate departureDate,
                                         int guestCount) {
        int validGuestCount = Math.max(guestCount, 1);

        StringBuilder sql = new StringBuilder("""
                SELECT DISTINCT
                    t.TourID,
                    t.LocationID,
                    t.CategoryID,
                    t.TourName,
                    t.Description,
                    t.BasePrice,
                    t.Duration,
                    t.Theme,
                    t.IsActive,
                    t.CreatedAt,
                    l.LocationName,
                    c.CategoryName
                FROM TOURS t
                JOIN LOCATIONS l ON l.LocationID = t.LocationID
                LEFT JOIN TOUR_CATEGORIES c ON c.CategoryID = t.CategoryID
                JOIN TOUR_SCHEDULES ts ON ts.TourID = t.TourID
                WHERE t.IsActive = 1
                  AND ts.ScheduleDate >= CAST(GETDATE() AS DATE)
                  AND (ts.AvailableSlots - ts.BookedSlots) >= ?
                """);

        List<Object> params = new ArrayList<>();
        params.add(validGuestCount);

        String cleanLocationKeyword = cleanString(locationKeyword);

        if (cleanLocationKeyword != null) {
            sql.append("""
                      AND (
                            l.LocationName LIKE ?
                         OR t.TourName LIKE ?
                         OR t.Description LIKE ?
                      )
                    """);

            String likeKeyword = "%" + cleanLocationKeyword + "%";
            params.add(likeKeyword);
            params.add(likeKeyword);
            params.add(likeKeyword);
        }

        if (departureDate != null) {
            sql.append(" AND ts.ScheduleDate = ? ");
            params.add(departureDate);
        }

        sql.append(" ORDER BY t.TourID ASC ");

        return queryTourList(
                sql.toString(),
                ps -> setParams(ps, params),
                "searchToursForHome"
        );
    }

    public List<Tour> filterTours(Integer locationId,
                                  Integer categoryId,
                                  Integer duration,
                                  BigDecimal minPrice,
                                  BigDecimal maxPrice,
                                  String theme) {
        StringBuilder sql = new StringBuilder(buildTourSql("WHERE t.IsActive = 1 "));
        List<Object> params = new ArrayList<>();

        if (locationId != null && locationId > 0) {
            sql.append(" AND t.LocationID = ? ");
            params.add(locationId);
        }

        if (categoryId != null && categoryId > 0) {
            sql.append(" AND t.CategoryID = ? ");
            params.add(categoryId);
        }

        if (duration != null && duration > 0) {
            sql.append(" AND t.Duration = ? ");
            params.add(duration);
        }

        if (minPrice != null && minPrice.compareTo(BigDecimal.ZERO) >= 0) {
            sql.append(" AND t.BasePrice >= ? ");
            params.add(minPrice);
        }

        if (maxPrice != null && maxPrice.compareTo(BigDecimal.ZERO) >= 0) {
            sql.append(" AND t.BasePrice <= ? ");
            params.add(maxPrice);
        }

        String cleanTheme = cleanString(theme);

        if (cleanTheme != null) {
            sql.append(" AND t.Theme LIKE ? ");
            params.add("%" + cleanTheme + "%");
        }

        sql.append(" ORDER BY t.TourID ASC ");

        return queryTourList(
                sql.toString(),
                ps -> setParams(ps, params),
                "filterTours"
        );
    }

    public int createTour(int locationId,
                          Integer categoryId,
                          String tourName,
                          String description,
                          BigDecimal basePrice,
                          int duration,
                          String theme) {
        TourInput input = validateTourInput(
                locationId,
                categoryId,
                tourName,
                description,
                basePrice,
                duration,
                theme
        );

        if (!input.valid) {
            System.out.println(input.message);
            return -1;
        }

        if (!isLocationExists(input.locationId)) {
            System.out.println("LocationID khong ton tai.");
            return -1;
        }

        if (input.categoryId != null && !isCategoryExists(input.categoryId)) {
            System.out.println("CategoryID khong ton tai.");
            return -1;
        }

        if (getTourByName(input.tourName) != null) {
            System.out.println("TourName da ton tai.");
            return -1;
        }

        String sql = """
                INSERT INTO TOURS
                (
                    LocationID,
                    CategoryID,
                    TourName,
                    Description,
                    BasePrice,
                    Duration,
                    Theme,
                    IsActive
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, 1)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)
        ) {
            ps.setInt(1, input.locationId);
            setNullableInt(ps, 2, input.categoryId);
            ps.setString(3, input.tourName);
            ps.setString(4, input.description);
            ps.setBigDecimal(5, input.basePrice);
            ps.setInt(6, input.duration);
            ps.setString(7, input.theme);

            int affectedRows = ps.executeUpdate();

            if (affectedRows > 0) {
                try (ResultSet rs = ps.getGeneratedKeys()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }

        } catch (SQLException e) {
            handleException(e, "createTour");
        }

        return -1;
    }

    public boolean updateTour(int tourId,
                              int locationId,
                              Integer categoryId,
                              String tourName,
                              String description,
                              BigDecimal basePrice,
                              int duration,
                              String theme) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return false;
        }

        Tour current = getTourById(tourId);

        if (current == null) {
            System.out.println("Khong tim thay tour.");
            return false;
        }

        TourInput input = validateTourInput(
                locationId,
                categoryId,
                tourName,
                description,
                basePrice,
                duration,
                theme
        );

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        if (!isLocationExists(input.locationId)) {
            System.out.println("LocationID khong ton tai.");
            return false;
        }

        if (input.categoryId != null && !isCategoryExists(input.categoryId)) {
            System.out.println("CategoryID khong ton tai.");
            return false;
        }

        Tour duplicated = getTourByName(input.tourName);

        if (duplicated != null && duplicated.getTourId() != tourId) {
            System.out.println("TourName da ton tai o tour khac.");
            return false;
        }

        String sql = """
                UPDATE TOURS
                SET LocationID = ?,
                    CategoryID = ?,
                    TourName = ?,
                    Description = ?,
                    BasePrice = ?,
                    Duration = ?,
                    Theme = ?
                WHERE TourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, input.locationId);
            setNullableInt(ps, 2, input.categoryId);
            ps.setString(3, input.tourName);
            ps.setString(4, input.description);
            ps.setBigDecimal(5, input.basePrice);
            ps.setInt(6, input.duration);
            ps.setString(7, input.theme);
            ps.setInt(8, tourId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Cap nhat tour that bai.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateTour");
        }

        return false;
    }

    public boolean activateTour(int tourId) {
        return updateActiveStatus(tourId, true);
    }

    public boolean deactivateTour(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return false;
        }

        if (hasFuturePaidOrPendingBookings(tourId)) {
            System.out.println("Tour dang co booking PENDING/PAID trong tuong lai, khong nen tat.");
            return false;
        }

        return updateActiveStatus(tourId, false);
    }

    public boolean deleteTour(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return false;
        }

        if (isTourReferenced(tourId)) {
            System.out.println("Khong the xoa tour da duoc tham chieu. Hay deactivate tour.");
            return false;
        }

        String sql = """
                DELETE FROM TOURS
                WHERE TourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay tour de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteTour");
        }

        return false;
    }

    public List<FeaturedTourView> getFeaturedTours() {
        String sql = """
                SELECT
                    FeaturedTourID,
                    TourID,
                    TourName,
                    Description,
                    BasePrice,
                    Duration,
                    Theme,
                    LocationName,
                    CategoryName,
                    DisplayOrder,
                    FeaturedTitle,
                    FeaturedDescription,
                    ImageURL
                FROM vw_FeaturedTours
                ORDER BY DisplayOrder ASC, FeaturedTourID ASC
                """;

        List<FeaturedTourView> featuredTours = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql);
                ResultSet rs = ps.executeQuery()
        ) {
            while (rs.next()) {
                featuredTours.add(mapFeaturedTourView(rs));
            }

        } catch (SQLException e) {
            handleException(e, "getFeaturedTours");
        }

        return featuredTours;
    }

    public List<PopularTourView> getPopularTours(int limit) {
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    TourID,
                    TourName,
                    Description,
                    BasePrice,
                    Duration,
                    Theme,
                    LocationName,
                    CategoryName,
                    TotalBookings,
                    TotalPassengers,
                    TotalOccupiedSlots,
                    TotalRevenue,
                    ImageURL
                FROM vw_PopularTours
                ORDER BY TotalPassengers DESC, TotalBookings DESC, TourID ASC
                """;

        List<PopularTourView> popularTours = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, validLimit);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    popularTours.add(mapPopularTourView(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, "getPopularTours");
        }

        return popularTours;
    }

    private Tour getTourByName(String tourName) {
        String cleanName = cleanString(tourName);

        if (cleanName == null) {
            return null;
        }

        String sql = buildTourSql("""
                WHERE t.TourName = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setString(1, cleanName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapTour(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getTourByName");
        }

        return null;
    }

    private boolean updateActiveStatus(int tourId, boolean active) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return false;
        }

        String sql = """
                UPDATE TOURS
                SET IsActive = ?
                WHERE TourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setBoolean(1, active);
            ps.setInt(2, tourId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay tour.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "updateActiveStatus");
        }

        return false;
    }

    private boolean hasFuturePaidOrPendingBookings(int tourId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS b
                JOIN TOUR_SCHEDULES ts ON ts.ScheduleID = b.ScheduleID
                WHERE b.TourID = ?
                  AND b.Status IN ('PENDING', 'PAID')
                  AND ts.ScheduleDate >= CAST(GETDATE() AS DATE)
                """;

        return queryInt(
                sql,
                ps -> ps.setInt(1, tourId),
                "hasFuturePaidOrPendingBookings"
        ) > 0;
    }

    private boolean isTourReferenced(int tourId) {
        String[] sqlStatements = {
                "SELECT COUNT(*) AS Total FROM TOUR_SCHEDULES WHERE TourID = ?",
                "SELECT COUNT(*) AS Total FROM TOUR_IMAGES WHERE TourID = ?",
                "SELECT COUNT(*) AS Total FROM TOUR_ITINERARY WHERE TourID = ?",
                "SELECT COUNT(*) AS Total FROM TOUR_LOCATIONS WHERE TourID = ?",
                "SELECT COUNT(*) AS Total FROM FEATURED_TOURS WHERE TourID = ?",
                "SELECT COUNT(*) AS Total FROM TOUR_PRICES WHERE TourID = ?",
                "SELECT COUNT(*) AS Total FROM BOOKINGS WHERE TourID = ?"
        };

        for (String sql : sqlStatements) {
            if (queryInt(
                    sql,
                    ps -> ps.setInt(1, tourId),
                    "isTourReferenced"
            ) > 0) {
                return true;
            }
        }

        return false;
    }

    private boolean isLocationExists(int locationId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM LOCATIONS
                WHERE LocationID = ?
                """;

        return queryInt(
                sql,
                ps -> ps.setInt(1, locationId),
                "isLocationExists"
        ) > 0;
    }

    private boolean isCategoryExists(int categoryId) {
        String sql = """
                SELECT COUNT(*) AS Total
                FROM TOUR_CATEGORIES
                WHERE CategoryID = ?
                """;

        return queryInt(
                sql,
                ps -> ps.setInt(1, categoryId),
                "isCategoryExists"
        ) > 0;
    }

    private int queryInt(String sql, SqlSetter setter, String methodName) {
        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt("Total");
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return 0;
    }

    private String buildTourSql(String condition) {
        return """
                SELECT
                    t.TourID,
                    t.LocationID,
                    t.CategoryID,
                    t.TourName,
                    t.Description,
                    t.BasePrice,
                    t.Duration,
                    t.Theme,
                    t.IsActive,
                    t.CreatedAt,
                    l.LocationName,
                    c.CategoryName
                FROM TOURS t
                JOIN LOCATIONS l ON l.LocationID = t.LocationID
                LEFT JOIN TOUR_CATEGORIES c ON c.CategoryID = t.CategoryID
                """ + condition;
    }

    private List<Tour> queryTourList(String sql,
                                     SqlSetter setter,
                                     String methodName) {
        List<Tour> tours = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    tours.add(mapTour(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return tours;
    }

    private Tour mapTour(ResultSet rs) throws SQLException {
        Tour tour = new Tour();

        tour.setTourId(rs.getInt("TourID"));
        tour.setLocationId(rs.getInt("LocationID"));

        int categoryId = rs.getInt("CategoryID");
        tour.setCategoryId(rs.wasNull() ? null : categoryId);

        tour.setTourName(rs.getString("TourName"));
        tour.setDescription(rs.getString("Description"));
        tour.setBasePrice(rs.getBigDecimal("BasePrice"));
        tour.setDuration(rs.getInt("Duration"));
        tour.setTheme(rs.getString("Theme"));
        tour.setActive(rs.getBoolean("IsActive"));

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            tour.setCreatedAt(createdAt.toLocalDateTime());
        }

        tour.setLocationName(rs.getString("LocationName"));
        tour.setCategoryName(rs.getString("CategoryName"));

        return tour;
    }

    private FeaturedTourView mapFeaturedTourView(ResultSet rs) throws SQLException {
        FeaturedTourView view = new FeaturedTourView();

        view.setFeaturedTourId(rs.getInt("FeaturedTourID"));
        view.setTourId(rs.getInt("TourID"));
        view.setTourName(rs.getString("TourName"));
        view.setDescription(rs.getString("Description"));
        view.setBasePrice(rs.getBigDecimal("BasePrice"));
        view.setDuration(rs.getInt("Duration"));
        view.setTheme(rs.getString("Theme"));
        view.setLocationName(rs.getString("LocationName"));
        view.setCategoryName(rs.getString("CategoryName"));
        view.setDisplayOrder(rs.getInt("DisplayOrder"));
        view.setFeaturedTitle(rs.getString("FeaturedTitle"));
        view.setFeaturedDescription(rs.getString("FeaturedDescription"));
        view.setImageUrl(rs.getString("ImageURL"));

        return view;
    }

    private PopularTourView mapPopularTourView(ResultSet rs) throws SQLException {
        PopularTourView view = new PopularTourView();

        view.setTourId(rs.getInt("TourID"));
        view.setTourName(rs.getString("TourName"));
        view.setDescription(rs.getString("Description"));
        view.setBasePrice(rs.getBigDecimal("BasePrice"));
        view.setDuration(rs.getInt("Duration"));
        view.setTheme(rs.getString("Theme"));
        view.setLocationName(rs.getString("LocationName"));
        view.setCategoryName(rs.getString("CategoryName"));
        view.setTotalBookings(rs.getInt("TotalBookings"));
        view.setTotalPassengers(rs.getInt("TotalPassengers"));
        view.setTotalOccupiedSlots(rs.getInt("TotalOccupiedSlots"));
        view.setTotalRevenue(rs.getBigDecimal("TotalRevenue"));
        view.setImageUrl(rs.getString("ImageURL"));

        return view;
    }

    private TourInput validateTourInput(int locationId,
                                        Integer categoryId,
                                        String tourName,
                                        String description,
                                        BigDecimal basePrice,
                                        int duration,
                                        String theme) {
        String cleanTourName = cleanString(tourName);
        String cleanDescription = cleanString(description);
        String cleanTheme = cleanString(theme);

        if (locationId <= 0) {
            return TourInput.invalid("LocationID khong hop le.");
        }

        if (categoryId != null && categoryId <= 0) {
            return TourInput.invalid("CategoryID khong hop le.");
        }

        if (cleanTourName == null || cleanTourName.length() > 200) {
            return TourInput.invalid("TourName khong hop le, toi da 200 ky tu.");
        }

        if (cleanDescription != null && cleanDescription.length() > 2000) {
            return TourInput.invalid("Description qua dai, toi da 2000 ky tu.");
        }

        if (basePrice == null || basePrice.compareTo(BigDecimal.ZERO) <= 0) {
            return TourInput.invalid("BasePrice phai lon hon 0.");
        }

        if (duration <= 0 || duration > 365) {
            return TourInput.invalid("Duration phai tu 1 den 365 ngay.");
        }

        if (cleanTheme != null && cleanTheme.length() > 100) {
            return TourInput.invalid("Theme qua dai, toi da 100 ky tu.");
        }

        return TourInput.valid(
                locationId,
                categoryId,
                cleanTourName,
                cleanDescription,
                basePrice,
                duration,
                cleanTheme
        );
    }

    private int normalizePageSize(int pageSize) {
        if (pageSize <= 0) {
            return 6;
        }

        return Math.min(pageSize, 50);
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 6;
        }

        return Math.min(limit, 50);
    }

    private void setParams(PreparedStatement ps, List<Object> params) throws SQLException {
        for (int index = 0; index < params.size(); index++) {
            Object value = params.get(index);
            int parameterIndex = index + 1;

            if (value instanceof Integer) {
                ps.setInt(parameterIndex, (Integer) value);
            } else if (value instanceof BigDecimal) {
                ps.setBigDecimal(parameterIndex, (BigDecimal) value);
            } else if (value instanceof LocalDate) {
                ps.setDate(parameterIndex, Date.valueOf((LocalDate) value));
            } else {
                ps.setString(parameterIndex, value.toString());
            }
        }
    }

    private void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, java.sql.Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private String cleanString(String value) {
        if (value == null) {
            return null;
        }

        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private void handleException(SQLException e, String methodName) {
        int errorCode = e.getErrorCode();
        String message = e.getMessage();

        if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": LocationID/CategoryID khong ton tai hoac tour dang duoc tham chieu.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": TourName hoac du lieu unique bi trung.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang TOURS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Du lieu vi pham CHECK constraint.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": Du lieu qua dai so voi cot trong database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang TOURS hoac view lien quan.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class TourInput {
        private final boolean valid;
        private final String message;
        private final int locationId;
        private final Integer categoryId;
        private final String tourName;
        private final String description;
        private final BigDecimal basePrice;
        private final int duration;
        private final String theme;

        private TourInput(boolean valid,
                          String message,
                          int locationId,
                          Integer categoryId,
                          String tourName,
                          String description,
                          BigDecimal basePrice,
                          int duration,
                          String theme) {
            this.valid = valid;
            this.message = message;
            this.locationId = locationId;
            this.categoryId = categoryId;
            this.tourName = tourName;
            this.description = description;
            this.basePrice = basePrice;
            this.duration = duration;
            this.theme = theme;
        }

        private static TourInput valid(int locationId,
                                       Integer categoryId,
                                       String tourName,
                                       String description,
                                       BigDecimal basePrice,
                                       int duration,
                                       String theme) {
            return new TourInput(
                    true,
                    null,
                    locationId,
                    categoryId,
                    tourName,
                    description,
                    basePrice,
                    duration,
                    theme
            );
        }

        private static TourInput invalid(String message) {
            return new TourInput(
                    false,
                    message,
                    0,
                    null,
                    null,
                    null,
                    null,
                    0,
                    null
            );
        }
    }
}
