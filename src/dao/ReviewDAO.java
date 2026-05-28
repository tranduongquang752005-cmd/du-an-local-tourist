package dao;

import config.DatabaseConnection;
import model.Review;

import java.math.BigDecimal;
import java.math.RoundingMode;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;

import java.util.ArrayList;
import java.util.List;

public class ReviewDAO {

    public boolean createReview(int userId, int bookingId, int rating, String reviewContent) {
        ReviewInput input = validateReviewInput(userId, bookingId, rating, reviewContent);

        if (!input.valid) {
            System.out.println(input.message);
            return false;
        }

        if (hasReviewed(input.userId, input.bookingId)) {
            System.out.println("User da danh gia booking nay roi.");
            return false;
        }

        /*
         * SQL trigger trg_Reviews_Guard sẽ kiểm tra thêm:
         * - booking thuộc đúng user
         * - booking.Status = COMPLETED
         */
        String sql = """
                INSERT INTO REVIEWS
                (
                    UserID,
                    BookingID,
                    Rating,
                    ReviewContent
                )
                VALUES (?, ?, ?, ?)
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, input.userId);
            ps.setInt(2, input.bookingId);
            ps.setInt(3, input.rating);
            ps.setString(4, input.reviewContent);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "createReview");
        }

        return false;
    }

    public boolean createReviewWithCriteria(int userId,
                                            int bookingId,
                                            int qualityRating,
                                            int transportRating,
                                            int serviceRating,
                                            String note) {
        if (!isValidRating(qualityRating)
                || !isValidRating(transportRating)
                || !isValidRating(serviceRating)) {
            System.out.println("Moi tieu chi phai trong khoang 1-5.");
            return false;
        }

        int avgRating = (int) Math.round((qualityRating + transportRating + serviceRating) / 3.0);
        String safeNote = escapeJson(note);

        String content = String.format(
                "{\"qualityRating\":%d,\"transportRating\":%d,\"serviceRating\":%d,\"note\":\"%s\"}",
                qualityRating,
                transportRating,
                serviceRating,
                safeNote
        );

        return createReview(userId, bookingId, avgRating, content);
    }

    public boolean hasReviewed(int userId, int bookingId) {
        if (userId <= 0 || bookingId <= 0) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM REVIEWS
                WHERE UserID = ?
                  AND BookingID = ?
                """;

        return queryInt(
                sql,
                ps -> {
                    ps.setInt(1, userId);
                    ps.setInt(2, bookingId);
                },
                "hasReviewed"
        ) > 0;
    }

    public boolean canReviewBooking(int userId, int bookingId) {
        if (userId <= 0 || bookingId <= 0) {
            return false;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM BOOKINGS b
                WHERE b.BookingID = ?
                  AND b.UserID = ?
                  AND b.Status = 'COMPLETED'
                  AND NOT EXISTS (
                      SELECT 1
                      FROM REVIEWS r
                      WHERE r.BookingID = b.BookingID
                        AND r.UserID = b.UserID
                  )
                """;

        return queryInt(
                sql,
                ps -> {
                    ps.setInt(1, bookingId);
                    ps.setInt(2, userId);
                },
                "canReviewBooking"
        ) > 0;
    }

    public Review getReviewById(int reviewId) {
        if (reviewId <= 0) {
            System.out.println("ReviewID khong hop le.");
            return null;
        }

        String sql = buildReviewSql("""
                WHERE r.ReviewID = ?
                """);

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, reviewId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapReview(rs);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getReviewById");
        }

        return null;
    }

    public List<Review> getRecentReviews(int limit) {
        int validLimit = normalizeLimit(limit);

        String sql = """
                SELECT TOP (?)
                    r.ReviewID,
                    r.UserID,
                    r.BookingID,
                    r.Rating,
                    r.ReviewContent,
                    r.ReviewDate,
                    r.CreatedAt,
                    u.FullName AS UserFullName,
                    t.TourName
                FROM REVIEWS r
                JOIN USERS u ON u.UserID = r.UserID
                JOIN BOOKINGS b ON b.BookingID = r.BookingID
                JOIN TOURS t ON t.TourID = b.TourID
                ORDER BY r.ReviewDate DESC, r.ReviewID DESC
                """;

        return queryReviewList(
                sql,
                ps -> ps.setInt(1, validLimit),
                "getRecentReviews"
        );
    }

    public List<Review> getReviewsByTourId(int tourId) {
        if (tourId <= 0) {
            System.out.println("TourID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildReviewSql("""
                WHERE b.TourID = ?
                ORDER BY r.ReviewDate DESC, r.ReviewID DESC
                """);

        return queryReviewList(
                sql,
                ps -> ps.setInt(1, tourId),
                "getReviewsByTourId"
        );
    }

    public List<Review> getReviewsByUserId(int userId) {
        if (userId <= 0) {
            System.out.println("UserID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildReviewSql("""
                WHERE r.UserID = ?
                ORDER BY r.ReviewDate DESC, r.ReviewID DESC
                """);

        return queryReviewList(
                sql,
                ps -> ps.setInt(1, userId),
                "getReviewsByUserId"
        );
    }

    public List<Review> getReviewsByBookingId(int bookingId) {
        if (bookingId <= 0) {
            System.out.println("BookingID khong hop le.");
            return new ArrayList<>();
        }

        String sql = buildReviewSql("""
                WHERE r.BookingID = ?
                ORDER BY r.ReviewDate DESC, r.ReviewID DESC
                """);

        return queryReviewList(
                sql,
                ps -> ps.setInt(1, bookingId),
                "getReviewsByBookingId"
        );
    }

    public BigDecimal getAverageRatingByTourId(int tourId) {
        if (tourId <= 0) {
            return BigDecimal.ZERO;
        }

        String sql = """
                SELECT AVG(CAST(r.Rating AS DECIMAL(4,2))) AS AverageRating
                FROM REVIEWS r
                JOIN BOOKINGS b ON b.BookingID = r.BookingID
                WHERE b.TourID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, tourId);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    BigDecimal avg = rs.getBigDecimal("AverageRating");
                    return avg == null
                            ? BigDecimal.ZERO
                            : avg.setScale(1, RoundingMode.HALF_UP);
                }
            }

        } catch (SQLException e) {
            handleException(e, "getAverageRatingByTourId");
        }

        return BigDecimal.ZERO;
    }

    public int countReviewsByTourId(int tourId) {
        if (tourId <= 0) {
            return 0;
        }

        String sql = """
                SELECT COUNT(*) AS Total
                FROM REVIEWS r
                JOIN BOOKINGS b ON b.BookingID = r.BookingID
                WHERE b.TourID = ?
                """;

        return queryInt(
                sql,
                ps -> ps.setInt(1, tourId),
                "countReviewsByTourId"
        );
    }

    public boolean updateReviewContent(int reviewId, int rating, String reviewContent) {
        String cleanContent = cleanString(reviewContent);

        if (reviewId <= 0) {
            System.out.println("ReviewID khong hop le.");
            return false;
        }

        if (!isValidRating(rating)) {
            System.out.println("Rating phai trong khoang 1-5.");
            return false;
        }

        if (cleanContent != null && cleanContent.length() > 4000) {
            System.out.println("ReviewContent qua dai, toi da 4000 ky tu.");
            return false;
        }

        if (getReviewById(reviewId) == null) {
            System.out.println("Khong tim thay review.");
            return false;
        }

        String sql = """
                UPDATE REVIEWS
                SET Rating = ?,
                    ReviewContent = ?
                WHERE ReviewID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, rating);
            ps.setString(2, cleanContent);
            ps.setInt(3, reviewId);

            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            handleException(e, "updateReviewContent");
        }

        return false;
    }

    public boolean deleteReview(int reviewId) {
        if (reviewId <= 0) {
            System.out.println("ReviewID khong hop le.");
            return false;
        }

        String sql = """
                DELETE FROM REVIEWS
                WHERE ReviewID = ?
                """;

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            ps.setInt(1, reviewId);

            int affectedRows = ps.executeUpdate();

            if (affectedRows == 0) {
                System.out.println("Khong tim thay review de xoa.");
                return false;
            }

            return true;

        } catch (SQLException e) {
            handleException(e, "deleteReview");
        }

        return false;
    }

    private String buildReviewSql(String condition) {
        return """
                SELECT
                    r.ReviewID,
                    r.UserID,
                    r.BookingID,
                    r.Rating,
                    r.ReviewContent,
                    r.ReviewDate,
                    r.CreatedAt,
                    u.FullName AS UserFullName,
                    t.TourName
                FROM REVIEWS r
                JOIN USERS u ON u.UserID = r.UserID
                JOIN BOOKINGS b ON b.BookingID = r.BookingID
                JOIN TOURS t ON t.TourID = b.TourID
                """ + condition;
    }

    private List<Review> queryReviewList(String sql,
                                         SqlSetter setter,
                                         String methodName) {
        List<Review> reviews = new ArrayList<>();

        try (
                Connection conn = DatabaseConnection.getConnection();
                PreparedStatement ps = conn.prepareStatement(sql)
        ) {
            if (setter != null) {
                setter.setParams(ps);
            }

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    reviews.add(mapReview(rs));
                }
            }

        } catch (SQLException e) {
            handleException(e, methodName);
        }

        return reviews;
    }

    private Review mapReview(ResultSet rs) throws SQLException {
        Review review = new Review();

        review.setReviewId(rs.getInt("ReviewID"));
        review.setUserId(rs.getInt("UserID"));
        review.setBookingId(rs.getInt("BookingID"));
        review.setRating(rs.getInt("Rating"));
        review.setReviewContent(rs.getString("ReviewContent"));

        Timestamp reviewDate = rs.getTimestamp("ReviewDate");
        if (reviewDate != null) {
            review.setReviewDate(reviewDate.toLocalDateTime());
        }

        Timestamp createdAt = rs.getTimestamp("CreatedAt");
        if (createdAt != null) {
            review.setCreatedAt(createdAt.toLocalDateTime());
        }

        review.setUserFullName(rs.getString("UserFullName"));
        review.setTourName(rs.getString("TourName"));

        return review;
    }

    private ReviewInput validateReviewInput(int userId,
                                            int bookingId,
                                            int rating,
                                            String reviewContent) {
        String cleanContent = cleanString(reviewContent);

        if (userId <= 0) {
            return ReviewInput.invalid("UserID khong hop le.");
        }

        if (bookingId <= 0) {
            return ReviewInput.invalid("BookingID khong hop le.");
        }

        if (!isValidRating(rating)) {
            return ReviewInput.invalid("Rating phai trong khoang 1-5.");
        }

        if (cleanContent != null && cleanContent.length() > 4000) {
            return ReviewInput.invalid("ReviewContent qua dai, toi da 4000 ky tu.");
        }

        return ReviewInput.valid(userId, bookingId, rating, cleanContent);
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

    private boolean isValidRating(int rating) {
        return rating >= 1 && rating <= 5;
    }

    private int normalizeLimit(int limit) {
        if (limit <= 0) {
            return 5;
        }

        return Math.min(limit, 100);
    }

    private String escapeJson(String value) {
        String cleanValue = cleanString(value);

        if (cleanValue == null) {
            return "";
        }

        return cleanValue
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r");
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

        if (errorCode == 50121) {
            System.out.println("Loi " + methodName + ": Chi duoc danh gia booking da COMPLETED va thuoc dung user.");
        } else if (errorCode == 547) {
            System.out.println("Loi " + methodName + ": UserID/BookingID khong ton tai hoac vi pham khoa ngoai.");
        } else if (errorCode == 2601 || errorCode == 2627) {
            System.out.println("Loi " + methodName + ": User da danh gia booking nay roi.");
        } else if (errorCode == 207) {
            System.out.println("Loi " + methodName + ": Sai ten cot trong SQL. Kiem tra bang REVIEWS.");
        } else if (message != null && message.contains("CHECK")) {
            System.out.println("Loi " + methodName + ": Rating phai trong khoang 1-5.");
        } else if (message != null && message.contains("String or binary data would be truncated")) {
            System.out.println("Loi " + methodName + ": ReviewContent qua dai so voi cot database.");
        } else if (message != null && message.contains("Invalid object name")) {
            System.out.println("Loi " + methodName + ": Khong tim thay bang REVIEWS.");
        } else {
            System.out.println("Loi " + methodName + "!");
            e.printStackTrace();
        }
    }

    private interface SqlSetter {
        void setParams(PreparedStatement ps) throws SQLException;
    }

    private static class ReviewInput {
        private final boolean valid;
        private final String message;
        private final int userId;
        private final int bookingId;
        private final int rating;
        private final String reviewContent;

        private ReviewInput(boolean valid,
                            String message,
                            int userId,
                            int bookingId,
                            int rating,
                            String reviewContent) {
            this.valid = valid;
            this.message = message;
            this.userId = userId;
            this.bookingId = bookingId;
            this.rating = rating;
            this.reviewContent = reviewContent;
        }

        private static ReviewInput valid(int userId,
                                         int bookingId,
                                         int rating,
                                         String reviewContent) {
            return new ReviewInput(
                    true,
                    null,
                    userId,
                    bookingId,
                    rating,
                    reviewContent
            );
        }

        private static ReviewInput invalid(String message) {
            return new ReviewInput(
                    false,
                    message,
                    0,
                    0,
                    0,
                    null
            );
        }
    }
}
