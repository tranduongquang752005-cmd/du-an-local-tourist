USE master;
GO
 
/* ============================================================
   FILE HOÀN THIỆN - DATABASE WEB DU LỊCH
   Các điểm đã chốt:
   - Java dùng java_user để kết nối SQL Server.
   - Phân quyền nghiệp vụ bằng USERS.Role: CUSTOMER / STAFF / MANAGER.
   - BOOKINGS.FinalPrice là computed column, Java không INSERT/UPDATE cột này.
   - TotalPrice không bao gồm SurchargeAmount; FinalPrice = TotalPrice - DiscountAmount + SurchargeAmount.
   - Trigger hỗ trợ thanh toán, vé, audit, add-on, huỷ đơn và cập nhật ghế.
   ============================================================ */

/* ============================================================
   DATABASE + LOGIN CHO JAVA
   Chạy file này sẽ xoá và tạo lại database DuLich_DB.
   Java kết nối bằng:
     user     = java_user
     password = 123456
     database = DuLich_DB
   ============================================================ */
 
IF NOT EXISTS (
    SELECT 1 FROM sys.server_principals WHERE name = 'java_user'
)
BEGIN
    CREATE LOGIN java_user WITH PASSWORD = '123456', CHECK_POLICY = OFF;
END
GO
 
ALTER LOGIN java_user WITH PASSWORD = '123456', CHECK_POLICY = OFF;
GO
 
IF DB_ID('DuLich_DB') IS NOT NULL
BEGIN
    ALTER DATABASE DuLich_DB SET SINGLE_USER WITH ROLLBACK IMMEDIATE;
    DROP DATABASE DuLich_DB;
END
GO
 
CREATE DATABASE DuLich_DB;
GO
 
USE DuLich_DB;
GO
 
CREATE USER java_user FOR LOGIN java_user;
GO
 
ALTER ROLE db_owner ADD MEMBER java_user;
GO
 
/* ============================================================
   1. PHÂN HỆ QUẢN LÝ TOUR & ĐỊA ĐIỂM
   ============================================================ */
 
-- Lưu trữ danh sách các tỉnh thành, điểm đến du lịch
CREATE TABLE LOCATIONS
(
    LocationID          INT           PRIMARY KEY IDENTITY(1,1),
    LocationName        NVARCHAR(50)  NOT NULL,
    DescriptionLocation NVARCHAR(1000),
    CreatedAt           DATETIME      NOT NULL DEFAULT GETDATE()
);
 
INSERT INTO LOCATIONS (LocationName, DescriptionLocation) VALUES
(N'Cần Thơ',   N'Thành phố duyên hải ĐBSCL'),
(N'Vĩnh Long', N'Tỉnh có du lịch sinh thái'),
(N'Bến Tre',   N'Vùng dừa nước nổi tiếng'),
(N'Cà Mau',    N'Mũi Cà Mau - mũi đất cực Nam'),
(N'An Giang',  N'Núi Sâm nổi tiếng'),
(N'Sóc Trăng', N'Chùa Dơi ấn tượng');
 
-- Lưu trữ các danh mục loại hình du lịch
CREATE TABLE TOUR_CATEGORIES
(
    CategoryID   INT           PRIMARY KEY IDENTITY(1,1),
    CategoryName NVARCHAR(100) NOT NULL UNIQUE,
    Description  NVARCHAR(500)
);
 
INSERT INTO TOUR_CATEGORIES (CategoryName, Description) VALUES
(N'Du lịch Sinh thái',   N'Khám phá thiên nhiên miệt vườn ĐBSCL'),
(N'Du lịch Tâm linh',    N'Tham quan các ngôi chùa và di tích lịch sử'),
(N'Du lịch Trải nghiệm', N'Tour trải nghiệm văn hoá, ẩm thực và đời sống địa phương');
 
-- Lưu trữ thông tin cốt lõi của gói tour
CREATE TABLE TOURS
(
    TourID      INT           PRIMARY KEY IDENTITY(1,1),
    LocationID  INT           NOT NULL,
    CategoryID  INT           NULL,
    TourName    NVARCHAR(100) NOT NULL,
    Description NVARCHAR(1000),
    BasePrice   DECIMAL(12,2) NOT NULL CHECK (BasePrice >= 0),
    Duration    TINYINT       NOT NULL CHECK (Duration BETWEEN 1 AND 30),
    Theme       NVARCHAR(50),
    IsActive    BIT           NOT NULL DEFAULT 1,
    CreatedAt   DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Tours_Locations   FOREIGN KEY (LocationID) REFERENCES LOCATIONS(LocationID),
    CONSTRAINT FK_Tours_Categories  FOREIGN KEY (CategoryID) REFERENCES TOUR_CATEGORIES(CategoryID)
);
 
INSERT INTO TOURS (LocationID, CategoryID, TourName, Description, BasePrice, Duration, Theme, IsActive) VALUES
(1, 1, N'Tour Cần Thơ - Miệt Vườn 1 Ngày',  N'Khám phá các miệt vườn trái cây ở Cần Thơ',    1000000, 1, N'Miệt vườn', 1),
(2, 1, N'Tour Vĩnh Long - Sinh Thái 2 Ngày', N'Tham quan đảo, chợ nổi và khu sinh thái',       1200000, 2, N'Sinh thái',  1),
(3, 1, N'Tour Bến Tre - Dừa Nước 1 Ngày',    N'Thăm các vùng dừa nước nổi tiếng',               900000, 1, N'Dừa nước',  1),
(4, 3, N'Tour Cà Mau - Mũi Cà Mau 2 Ngày',  N'Đến mũi đất cực Nam Việt Nam',                  1500000, 2, N'Phiêu lưu', 1),
(5, 2, N'Tour An Giang - Núi Sâm 1 Ngày',    N'Tham quan núi Sâm, chùa Tây An',                 800000, 1, N'Tâm linh',  1),
(6, 2, N'Tour Sóc Trăng - Chùa Dơi 1 Ngày', N'Thăm chùa Dơi độc đáo',                         950000, 1, N'Tâm linh',  1);
 
-- Lưu trữ lộ trình các điểm dừng chân của tour theo từng ngày
CREATE TABLE TOUR_LOCATIONS
(
    TourLocationID INT          PRIMARY KEY IDENTITY(1,1),
    TourID         INT          NOT NULL,
    LocationID     INT          NOT NULL,
    DayNumber      TINYINT      NOT NULL CHECK (DayNumber > 0),
    SequenceOrder  TINYINT      NOT NULL CHECK (SequenceOrder > 0),
    Description    NVARCHAR(1000),
    CreatedAt      DATETIME     NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_TourLocations_Tours     FOREIGN KEY (TourID)     REFERENCES TOURS(TourID),
    CONSTRAINT FK_TourLocations_Locations FOREIGN KEY (LocationID) REFERENCES LOCATIONS(LocationID),
    CONSTRAINT UQ_TourLocations_Order     UNIQUE (TourID, DayNumber, SequenceOrder)
);
 
INSERT INTO TOUR_LOCATIONS (TourID, LocationID, DayNumber, SequenceOrder, Description) VALUES
(1, 1, 1, 1, N'Tham quan miệt vườn Cần Thơ'),
(2, 2, 1, 1, N'Tham quan khu sinh thái Vĩnh Long'),
(3, 3, 1, 1, N'Trải nghiệm sông nước Bến Tre'),
(4, 4, 1, 1, N'Khởi hành đến Cà Mau'),
(4, 4, 2, 1, N'Tham quan Mũi Cà Mau'),
(5, 5, 1, 1, N'Tham quan Núi Sâm'),
(6, 6, 1, 1, N'Tham quan Chùa Dơi');
 
-- Lưu trữ danh sách đường dẫn hình ảnh quảng bá của từng tour
CREATE TABLE TOUR_IMAGES
(
    ImageID   INT           PRIMARY KEY IDENTITY(1,1),
    TourID    INT           NOT NULL,
    ImageURL  NVARCHAR(500) NOT NULL,
    IsActive  BIT           NOT NULL DEFAULT 1,
    CreatedAt DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_TourImages_Tours FOREIGN KEY (TourID) REFERENCES TOURS(TourID)
);
 
INSERT INTO TOUR_IMAGES (TourID, ImageURL, IsActive) VALUES
(1, N'/images/tours/can-tho.jpg',   1),
(2, N'/images/tours/vinh-long.jpg', 1),
(3, N'/images/tours/ben-tre.jpg',   1),
(4, N'/images/tours/ca-mau.jpg',    1),
(5, N'/images/tours/an-giang.jpg',  1),
(6, N'/images/tours/soc-trang.jpg', 1);
 
-- Lưu trữ mốc thời gian và hoạt động chi tiết trong ngày của tour
CREATE TABLE TOUR_ITINERARY
(
    ItineraryID INT           PRIMARY KEY IDENTITY(1,1),
    TourID      INT           NOT NULL,
    DayNumber   TINYINT       NOT NULL CHECK (DayNumber > 0),
    TimeStart   TIME,
    TimeEnd     TIME,
    Activity    NVARCHAR(100),
    Description NVARCHAR(1000),
    CreatedAt   DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_TourItinerary_Tours FOREIGN KEY (TourID) REFERENCES TOURS(TourID),
    CONSTRAINT CHK_Itinerary_Time CHECK (TimeStart IS NULL OR TimeEnd IS NULL OR TimeStart < TimeEnd)
);
 
INSERT INTO TOUR_ITINERARY (TourID, DayNumber, TimeStart, TimeEnd, Activity, Description) VALUES
(1, 1, '07:00', '08:00', N'Khởi hành',          N'Tập trung và khởi hành'),
(1, 1, '09:00', '11:00', N'Tham quan miệt vườn', N'Thưởng thức trái cây địa phương'),
(2, 1, '08:00', '10:00', N'Chợ nổi',             N'Tham quan chợ nổi và đời sống sông nước'),
(5, 1, '08:00', '11:00', N'Tham quan Núi Sâm',   N'Viếng chùa Tây An và các điểm tâm linh');
 
/* ============================================================
   2. PHÂN HỆ LỊCH TRÌNH & ĐIỀU PHỐI
   ============================================================ */
 
-- Lưu trữ lịch sử biến động giá xăng dầu
CREATE TABLE FUEL_PRICES
(
    FuelPriceID   INT           PRIMARY KEY IDENTITY(1,1),
    Price         DECIMAL(12,2) NOT NULL CHECK (Price > 0),
    EffectiveDate DATE          NOT NULL UNIQUE,
    CreatedAt     DATETIME      NOT NULL DEFAULT GETDATE()
);
 
INSERT INTO FUEL_PRICES (Price, EffectiveDate) VALUES
(25000, '2026-07-01'),
(25800, '2026-07-02'),
(26500, '2026-07-05'),
(26200, '2026-07-08'),
(27200, '2026-07-10');
 
-- Lưu trữ các chuyến khởi hành thực tế theo ngày
CREATE TABLE TOUR_SCHEDULES
(
    ScheduleID      INT           PRIMARY KEY IDENTITY(1,1),
    TourID          INT           NOT NULL,
    ScheduleDate    DATE          NOT NULL,
    AvailableSlots  SMALLINT      NOT NULL CHECK (AvailableSlots > 0),
    BookedSlots     SMALLINT      NOT NULL DEFAULT 0 CHECK (BookedSlots >= 0),
    PriceMultiplier DECIMAL(4,2)  NOT NULL DEFAULT 1.00 CHECK (PriceMultiplier > 0),
    Surcharge       DECIMAL(12,2) NOT NULL DEFAULT 0    CHECK (Surcharge >= 0),
    FuelPriceID     INT           NULL,
    CreatedAt       DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_TourSchedules_Tours      FOREIGN KEY (TourID)      REFERENCES TOURS(TourID),
    CONSTRAINT FK_TourSchedules_FuelPrices FOREIGN KEY (FuelPriceID) REFERENCES FUEL_PRICES(FuelPriceID),
    CONSTRAINT UQ_TourSchedule_TourDate    UNIQUE (TourID, ScheduleDate),
    CONSTRAINT UQ_TourSchedules_ID_Tour    UNIQUE (ScheduleID, TourID),   -- hỗ trợ FK composite từ BOOKINGS
    CONSTRAINT CHK_TourSchedules_BookedSlots CHECK (BookedSlots <= AvailableSlots)
);
 
INSERT INTO TOUR_SCHEDULES (TourID, ScheduleDate, AvailableSlots, BookedSlots, PriceMultiplier, Surcharge, FuelPriceID) VALUES
(1, '2026-07-01', 30, 0, 1.00, 0,      1),
(1, '2026-07-05', 30, 0, 1.00, 0,      3),
(1, '2026-07-10', 30, 0, 1.20, 200000, 5),
(2, '2026-07-02', 25, 0, 1.00, 0,      2),
(2, '2026-07-08', 25, 0, 1.00, 0,      4),
(3, '2026-07-03', 20, 0, 1.00, 0,      1),
(3, '2026-07-12', 20, 0, 1.00, 0,      3),
(4, '2026-07-06', 15, 0, 1.00, 0,      5),
(4, '2026-07-15', 15, 0, 1.00, 0,      2),
(5, '2026-07-04', 35, 0, 1.00, 0,      4),
(6, '2026-07-09', 30, 0, 1.00, 0,      4);
 
-- Lưu trữ các quy định tăng/giảm giá tự động
CREATE TABLE DYNAMIC_PRICE_RULES
(
    RuleID          INT           PRIMARY KEY IDENTITY(1,1),
    RuleName        NVARCHAR(100) NOT NULL,
    ConditionType   NVARCHAR(50)  NOT NULL CHECK (ConditionType IN ('HOLIDAY','WEEKEND','FUEL_SURGE','LOW_STOCK')),
    ModifierPercent DECIMAL(5,2)  NOT NULL,
    StartDate       DATE,
    EndDate         DATE,
    Priority        TINYINT       NOT NULL CHECK (Priority BETWEEN 1 AND 10),
    IsActive        BIT           NOT NULL DEFAULT 1,
    CreatedAt       DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT CHK_DynamicRules_DateRange CHECK (StartDate IS NULL OR EndDate IS NULL OR StartDate <= EndDate)
);
 
INSERT INTO DYNAMIC_PRICE_RULES (RuleName, ConditionType, ModifierPercent, StartDate, EndDate, Priority, IsActive) VALUES
(N'Mùa cao điểm tháng 7', 'HOLIDAY',    20, '2026-07-08', '2026-07-15', 1, 1),
(N'Cuối tuần',             'WEEKEND',    15, NULL,         NULL,         2, 1),
(N'Xăng tăng 10%',         'FUEL_SURGE', 10, '2026-07-05', '2026-07-31', 1, 1),
(N'Ghế sắp hết',           'LOW_STOCK',  25, NULL,         NULL,         3, 1);
 
-- Bảng trung gian liên kết quy định giá động vào từng lịch trình
CREATE TABLE SCHEDULE_DYNAMIC_RULES
(
    ScheduleRuleID INT      PRIMARY KEY IDENTITY(1,1),
    ScheduleID     INT      NOT NULL,
    RuleID         INT      NOT NULL,
    CreatedAt      DATETIME NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ScheduleDynamicRules_Schedules FOREIGN KEY (ScheduleID) REFERENCES TOUR_SCHEDULES(ScheduleID),
    CONSTRAINT FK_ScheduleDynamicRules_Rules     FOREIGN KEY (RuleID)     REFERENCES DYNAMIC_PRICE_RULES(RuleID),
    CONSTRAINT UQ_ScheduleDynamicRules           UNIQUE (ScheduleID, RuleID)
);



 
-- Lưu trữ lịch sử thay đổi giá gốc của từng tour theo mốc thời gian
CREATE TABLE TOUR_PRICES
(
    PriceID       INT           PRIMARY KEY IDENTITY(1,1),
    TourID        INT           NOT NULL,
    EffectiveDate DATE          NOT NULL,
    Price         DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    Reason        NVARCHAR(255),
    CreatedAt     DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_TourPrices_Tours       FOREIGN KEY (TourID) REFERENCES TOURS(TourID),
    CONSTRAINT UQ_TourPrices_TourDate    UNIQUE (TourID, EffectiveDate)
);
 
/* ============================================================
   3. PHÂN HỆ NGƯỜI DÙNG & ĐƠN HÀNG
   Phân quyền:
     CUSTOMER = Khách hàng
     STAFF    = Nhân viên: thêm/sửa/xoá dữ liệu thường
     MANAGER  = Trưởng phòng: xem toàn bộ, bao gồm dữ liệu nhạy cảm
   ============================================================ */
 
CREATE TABLE USERS
(
    UserID             INT           PRIMARY KEY IDENTITY(1,1),
    FullName           NVARCHAR(500) NOT NULL,
    Phone              VARCHAR(10)   NOT NULL UNIQUE
                           CHECK (LEN(Phone)=10 AND Phone NOT LIKE '%[^0-9]%' AND Phone LIKE '0%'),
    PasswordHash       NVARCHAR(255) NOT NULL CHECK (LEN(PasswordHash) >= 60),
    /* FIX: đổi từ ADMIN → STAFF / MANAGER để tách bạch quyền nghiệp vụ */
    Role               NVARCHAR(20)  NOT NULL CHECK (Role IN ('CUSTOMER','STAFF','MANAGER')),
    IsActive           BIT           NOT NULL DEFAULT 1,
    MaxPendingBookings INT           NOT NULL DEFAULT 3 CHECK (MaxPendingBookings BETWEEN 1 AND 10),
    CreatedAt          DATETIME      NOT NULL DEFAULT GETDATE()

);

 
INSERT INTO USERS (FullName, Phone, PasswordHash, Role, IsActive) VALUES
(N'Trưởng phòng', '0901111111', REPLICATE('x',60), 'MANAGER',  1),
(N'Nhân viên A',  '0902222222', REPLICATE('x',60), 'STAFF',    1),
(N'Nguyễn Văn A', '0903333333', REPLICATE('x',60), 'CUSTOMER', 1),
(N'Trần Thị B',   '0904444444', REPLICATE('x',60), 'CUSTOMER', 1),
(N'Lê Văn C',     '0905555555', REPLICATE('x',60), 'CUSTOMER', 1),
(N'Phạm Thị D',   '0906666666', REPLICATE('x',60), 'CUSTOMER', 1),
(N'Hoàng Văn E',  '0907777777', REPLICATE('x',60), 'CUSTOMER', 1),
(N'Đặng Văn G',   '0908888888', REPLICATE('x',60), 'CUSTOMER', 1);

/* ============================================================
   BẢNG TOUR NỔI BẬT
   Dùng để nhân viên/trưởng phòng chọn tour hiển thị nổi bật
   trên trang chủ hoặc khu vực đề xuất.
   ============================================================ */

CREATE TABLE FEATURED_TOURS
(
    FeaturedTourID INT PRIMARY KEY IDENTITY(1,1),

    -- Tour được chọn làm nổi bật
    TourID INT NOT NULL,

    -- Thứ tự hiển thị trên giao diện
    DisplayOrder INT NOT NULL DEFAULT 1 CHECK (DisplayOrder > 0),

    -- Tiêu đề/ mô tả riêng cho phần nổi bật
    FeaturedTitle NVARCHAR(150) NULL,
    FeaturedDescription NVARCHAR(500) NULL,

    -- Khoảng thời gian áp dụng nổi bật
    StartDate DATE NULL,
    EndDate DATE NULL,

    IsActive BIT NOT NULL DEFAULT 1,

    -- Người tạo, thường là STAFF hoặc MANAGER
    CreatedBy INT NULL,

    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME NULL,

    CONSTRAINT FK_FeaturedTours_Tours
        FOREIGN KEY (TourID) REFERENCES TOURS(TourID),

    CONSTRAINT FK_FeaturedTours_Users
        FOREIGN KEY (CreatedBy) REFERENCES USERS(UserID),

    CONSTRAINT UQ_FeaturedTours_Tour
        UNIQUE (TourID),

    CONSTRAINT CHK_FeaturedTours_DateRange
        CHECK (StartDate IS NULL OR EndDate IS NULL OR StartDate <= EndDate)
);
GO

CREATE INDEX IDX_FeaturedTours_IsActive
ON FEATURED_TOURS(IsActive);

CREATE INDEX IDX_FeaturedTours_DisplayOrder
ON FEATURED_TOURS(DisplayOrder);

CREATE INDEX IDX_FeaturedTours_DateRange
ON FEATURED_TOURS(StartDate, EndDate);

CREATE INDEX IDX_FeaturedTours_TourID
ON FEATURED_TOURS(TourID);
GO

/* ========================= DỮ LIỆU MẪU TOUR NỔI BẬT ========================= */

INSERT INTO FEATURED_TOURS 
(
    TourID, 
    DisplayOrder, 
    FeaturedTitle, 
    FeaturedDescription, 
    StartDate, 
    EndDate, 
    IsActive, 
    CreatedBy
)
VALUES
(
    1, 
    1, 
    N'Tour miền Tây được yêu thích', 
    N'Khám phá miệt vườn Cần Thơ trong 1 ngày', 
    NULL, 
    NULL, 
    1, 
    1
),
(
    4, 
    2, 
    N'Hành trình đến cực Nam', 
    N'Trải nghiệm Mũi Cà Mau trong 2 ngày', 
    NULL, 
    NULL, 
    1, 
    1
),
(
    5, 
    3, 
    N'Tour tâm linh nổi bật', 
    N'Tham quan Núi Sâm và chùa Tây An', 
    NULL, 
    NULL, 
    1, 
    1
);
GO
 
-- Lưu trữ cấu hình mã giảm giá toàn hệ thống
CREATE TABLE COUPONS
(
    CouponID          INT           PRIMARY KEY IDENTITY(1,1),
    CouponCode        NVARCHAR(50)  NOT NULL UNIQUE,
    DiscountType      NVARCHAR(20)  NOT NULL CHECK (DiscountType IN ('PERCENTAGE','FIXED')),
    DiscountValue     DECIMAL(12,2) NOT NULL CHECK (DiscountValue > 0),
    MaxUsagePerUser   INT           NOT NULL DEFAULT 1 CHECK (MaxUsagePerUser > 0),
    MaxTotalUsage     INT           NULL,
    CurrentTotalUsage INT           NOT NULL DEFAULT 0,
    MaxDiscountAmount DECIMAL(12,2) NULL,
    ExpiryDate        DATE          NOT NULL,
    IsActive          BIT           NOT NULL DEFAULT 1,
    CreatedBy         INT           NULL,
    CreatedAt         DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Coupons_Users    FOREIGN KEY (CreatedBy) REFERENCES USERS(UserID),
    CONSTRAINT CHK_Coupons_Discount CHECK (
        (DiscountType='PERCENTAGE' AND DiscountValue <= 100) OR DiscountType='FIXED'
    ),
    CONSTRAINT CHK_Coupons_TotalUsage CHECK (
        MaxTotalUsage IS NULL OR (CurrentTotalUsage >= 0 AND CurrentTotalUsage <= MaxTotalUsage)
    )
);
 
INSERT INTO COUPONS (CouponCode, DiscountType, DiscountValue, MaxUsagePerUser, MaxTotalUsage, CurrentTotalUsage, MaxDiscountAmount, ExpiryDate, IsActive, CreatedBy) VALUES
(N'SUMMER20',  'PERCENTAGE', 20,     1, 100,  0, 500000, '2026-08-31', 1, 1),
(N'EASTER15',  'PERCENTAGE', 15,     1, 200,  0, 300000, '2026-07-31', 1, 1),
(N'FIXED200',  'FIXED',      200000, 1, 50,   0, NULL,   '2026-07-31', 1, 1),
(N'WELCOME10', 'PERCENTAGE', 10,     1, NULL, 0, 200000, '2026-12-31', 1, 1),
(N'VIP300',    'FIXED',      300000, 1, 10,   0, NULL,   '2026-07-31', 1, 1);
 
-- Lưu trữ đơn đặt tour của khách
-- FIX: FinalPrice là computed column PERSISTED → Java KHÔNG set giá trị này, chỉ đọc
CREATE TABLE BOOKINGS
(
    BookingID       INT           PRIMARY KEY IDENTITY(1,1),
    UserID          INT           NOT NULL,
    TourID          INT           NOT NULL,
    ScheduleID      INT           NOT NULL,
    BookingDate     DATETIME      NOT NULL DEFAULT GETDATE(),
    TotalPrice      DECIMAL(12,2) NOT NULL CHECK (TotalPrice >= 0),
    CouponID        INT           NULL,
    DiscountAmount  DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (DiscountAmount >= 0),
    SurchargeAmount DECIMAL(12,2) NOT NULL DEFAULT 0 CHECK (SurchargeAmount >= 0),
    /* Computed: Java chỉ SELECT, không INSERT/UPDATE cột này */
    FinalPrice      AS (TotalPrice - DiscountAmount + SurchargeAmount) PERSISTED,
    Status          NVARCHAR(20)  NOT NULL CHECK (Status IN ('PENDING','PAID','COMPLETED','CANCELLED')),
    CreatedAt       DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Bookings_Users     FOREIGN KEY (UserID)    REFERENCES USERS(UserID),
    CONSTRAINT FK_Bookings_Tours     FOREIGN KEY (TourID)    REFERENCES TOURS(TourID),
    CONSTRAINT FK_Bookings_Coupons   FOREIGN KEY (CouponID)  REFERENCES COUPONS(CouponID) ON DELETE SET NULL,
    /* FK composite đảm bảo ScheduleID thuộc đúng TourID */
    CONSTRAINT FK_Bookings_Schedules FOREIGN KEY (ScheduleID, TourID) REFERENCES TOUR_SCHEDULES(ScheduleID, TourID),
    CONSTRAINT CHK_Bookings_Discount CHECK (DiscountAmount <= TotalPrice)
);
 
/* ============================================================
   BẢNG YÊU CẦU VẬN CHUYỂN
   Bộ phận mình không trực tiếp điều phối xe.
   Hệ thống chỉ tiếp nhận thông tin, tạo yêu cầu vận chuyển,
   sau đó chuyển cho đối tác/bộ phận khác xử lý.
   ============================================================ */

CREATE TABLE TRANSPORT_REQUESTS
(
    TransportRequestID INT PRIMARY KEY IDENTITY(1,1),

    -- Lịch trình tour cần vận chuyển
    ScheduleID INT NOT NULL,

    -- Có thể gắn với booking cụ thể nếu yêu cầu phát sinh sau khi khách đặt tour
    BookingID INT NULL,

    -- Thông tin đối tác/bên nhận điều phối xe
    PartnerName NVARCHAR(100) NULL,
    ContactPhone VARCHAR(15) NULL,

    -- Thông tin điểm đón/trả
    PickupLocation NVARCHAR(500) NULL,
    DropoffLocation NVARCHAR(500) NULL,

    -- Số lượng khách cần vận chuyển
    PassengerCount INT NOT NULL CHECK (PassengerCount > 0),

    -- Ghi chú thêm cho bên điều phối
    Note NVARCHAR(1000) NULL,

    -- Trạng thái xử lý yêu cầu
    Status NVARCHAR(20) NOT NULL DEFAULT 'PENDING'
        CHECK (Status IN ('PENDING','SENT','CONFIRMED','CANCELLED')),

    -- Nhân viên tạo yêu cầu
    CreatedBy INT NULL,

    CreatedAt DATETIME NOT NULL DEFAULT GETDATE(),
    UpdatedAt DATETIME NULL,

    CONSTRAINT FK_TransportRequests_Schedules
        FOREIGN KEY (ScheduleID) REFERENCES TOUR_SCHEDULES(ScheduleID),

    CONSTRAINT FK_TransportRequests_Bookings
        FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID),

    CONSTRAINT FK_TransportRequests_Users
        FOREIGN KEY (CreatedBy) REFERENCES USERS(UserID)
);
GO

/* ========================= INDEXES CHO TRANSPORT_REQUESTS ========================= */

CREATE INDEX IDX_TransportRequests_ScheduleID
ON TRANSPORT_REQUESTS(ScheduleID);

CREATE INDEX IDX_TransportRequests_BookingID
ON TRANSPORT_REQUESTS(BookingID);

CREATE INDEX IDX_TransportRequests_Status
ON TRANSPORT_REQUESTS(Status);

CREATE INDEX IDX_TransportRequests_CreatedAt
ON TRANSPORT_REQUESTS(CreatedAt);
GO

-- Lưu trữ danh sách hành khách trong đơn hàng
-- Giá vé: ADULT = 100%, CHILD = 50%, BABY = 25% (xem sp_GetTourPrice)
CREATE TABLE BOOKING_PASSENGERS
(
    PassengerID   INT           PRIMARY KEY IDENTITY(1,1),
    BookingID     INT           NOT NULL,
    PassengerName NVARCHAR(100) NOT NULL,
    PassengerType NVARCHAR(20)  NOT NULL CHECK (PassengerType IN ('ADULT','CHILD','BABY')),
    Price         DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    SlotsOccupied SMALLINT      NOT NULL DEFAULT 1 CHECK (SlotsOccupied >= 0),
    CreatedAt     DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_BookingPassengers_Bookings FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID),
    CONSTRAINT CHK_BookingPassengers_Price CHECK (
        (PassengerType IN ('ADULT','CHILD') AND Price > 0) OR (PassengerType='BABY' AND Price >= 0)
    ),
    CONSTRAINT CHK_BookingPassengers_Slots CHECK (
        (PassengerType IN ('ADULT','CHILD') AND SlotsOccupied = 1) OR (PassengerType='BABY' AND SlotsOccupied = 0)
    )
);
 
-- Lưu trữ các dịch vụ mua thêm (áo bà ba, xe đưa đón...)
CREATE TABLE ADD_ONS
(
    AddOnID     INT           PRIMARY KEY IDENTITY(1,1),
    AddOnName   NVARCHAR(100) NOT NULL,
    Price       DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    Description NVARCHAR(1000),
    IsActive    BIT           NOT NULL DEFAULT 1,
    CreatedAt   DATETIME      NOT NULL DEFAULT GETDATE()
);
 
INSERT INTO ADD_ONS (AddOnName, Price, Description, IsActive) VALUES
(N'Thuê áo bà ba',        50000,  N'Bộ áo bà ba truyền thống ĐBSCL',               1),
(N'Xe đưa đón',           200000, N'Dịch vụ xe riêng đưa đón từ TP.HCM',           1),
(N'Ăn sáng bổ sung',       80000, N'Bữa sáng thêm với đặc sản địa phương',         1),
(N'Bảo hiểm du lịch',     120000, N'Bảo hiểm tai nạn toàn diện cho tour',          1),
(N'Hướng dẫn viên riêng', 300000, N'Hướng dẫn viên chuyên biệt cho nhóm',         1);
 
-- Lưu trữ dịch vụ cộng thêm khách đã chọn trong đơn hàng
CREATE TABLE BOOKING_ADD_ONS
(
    BookingAddOnID INT           PRIMARY KEY IDENTITY(1,1),
    BookingID      INT           NOT NULL,
    AddOnID        INT           NOT NULL,
    Quantity       TINYINT       NOT NULL DEFAULT 1 CHECK (Quantity > 0),
    Price          DECIMAL(12,2) NOT NULL CHECK (Price >= 0),
    CreatedAt      DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_BookingAddOns_Bookings FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID),
    CONSTRAINT FK_BookingAddOns_AddOns   FOREIGN KEY (AddOnID)   REFERENCES ADD_ONS(AddOnID),
    CONSTRAINT UQ_BookingAddOns_Booking_AddOn UNIQUE (BookingID, AddOnID)
);
 
-- Lưu trữ đánh giá của khách sau khi hoàn thành tour
CREATE TABLE REVIEWS
(
    ReviewID      INT        PRIMARY KEY IDENTITY(1,1),
    UserID        INT        NOT NULL,
    BookingID     INT        NOT NULL,
    Rating        TINYINT    NOT NULL CHECK (Rating BETWEEN 1 AND 5),
    ReviewContent NVARCHAR(MAX),
    ReviewDate    DATETIME   NOT NULL DEFAULT GETDATE(),
    CreatedAt     DATETIME   NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Reviews_Users    FOREIGN KEY (UserID)    REFERENCES USERS(UserID),
    CONSTRAINT FK_Reviews_Bookings FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID),
    CONSTRAINT UQ_Reviews_User_Booking UNIQUE (UserID, BookingID)
);
 
/* ============================================================
   4. PHÂN HỆ THANH TOÁN & HUỶ ĐƠN
   ============================================================ */
 
-- Lưu trữ lịch sử các lượt thanh toán của từng đơn hàng
CREATE TABLE PAYMENTS
(
    PaymentID     INT           PRIMARY KEY IDENTITY(1,1),
    BookingID     INT           NOT NULL,
    Amount        DECIMAL(12,2) NOT NULL CHECK (Amount > 0),
    PaymentMethod NVARCHAR(50)  NOT NULL,
    TransactionID NVARCHAR(100),
    PaymentStatus NVARCHAR(20)  NOT NULL CHECK (PaymentStatus IN ('PENDING','SUCCESS','FAILED')),
    PaymentDate   DATETIME      NOT NULL DEFAULT GETDATE(),
    CreatedAt     DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Payments_Bookings FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID)
);
 
-- Một booking chỉ có tối đa 1 lần thanh toán SUCCESS
CREATE UNIQUE INDEX UIX_Payments_SuccessBooking ON PAYMENTS(BookingID) WHERE PaymentStatus='SUCCESS';
 
-- Lưu trữ mã vé điện tử phục vụ điểm danh
CREATE TABLE E_TICKETS
(
    TicketID     INT           PRIMARY KEY IDENTITY(1,1),
    BookingID    INT           NOT NULL UNIQUE,
    TicketCode   NVARCHAR(50)  NOT NULL UNIQUE,
    QRCode       NVARCHAR(500),
    TicketStatus NVARCHAR(20)  NOT NULL CHECK (TicketStatus IN ('ACTIVE','USED','EXPIRED')),
    IssuedDate   DATETIME      NOT NULL DEFAULT GETDATE(),
    ExpiryDate   DATETIME,
    CreatedAt    DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_ETickets_Bookings FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID),
    CONSTRAINT CHK_ETickets_Expiry  CHECK (ExpiryDate IS NULL OR ExpiryDate > IssuedDate)
);
 
-- Lưu trữ thông tin công ty hủy chuyến và phương án đền bù
CREATE TABLE TOUR_CANCELLATIONS
(
    TourCancelID   INT           PRIMARY KEY IDENTITY(1,1),
    ScheduleID     INT           NOT NULL,
    CancelReason   NVARCHAR(500) NOT NULL,
    ResolutionType NVARCHAR(20)  NOT NULL CHECK (ResolutionType IN ('RESCHEDULE','REFUND')),
    NewScheduleID  INT           NULL,
    RefundPercent  DECIMAL(5,2)  NULL CHECK (RefundPercent BETWEEN 0 AND 100),
    CreatedAt      DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_TourCancellations_Schedules    FOREIGN KEY (ScheduleID)    REFERENCES TOUR_SCHEDULES(ScheduleID),
    CONSTRAINT FK_TourCancellations_NewSchedules FOREIGN KEY (NewScheduleID) REFERENCES TOUR_SCHEDULES(ScheduleID),
    CONSTRAINT CHK_TourCancellation_Logic CHECK (
        (ResolutionType='RESCHEDULE' AND NewScheduleID IS NOT NULL AND RefundPercent IS NULL)
        OR (ResolutionType='REFUND'    AND NewScheduleID IS NULL    AND RefundPercent IS NOT NULL)
    )
);
 
-- Mỗi lịch trình chỉ hủy 1 lần
CREATE UNIQUE INDEX UQ_TourCancellations_ScheduleID ON TOUR_CANCELLATIONS(ScheduleID);
 
-- Lưu trữ lý do và tỷ lệ hoàn tiền khi hủy đơn hàng
CREATE TABLE BOOKING_CANCELLATIONS
(
    BookingCancelID INT           PRIMARY KEY IDENTITY(1,1),
    BookingID       INT           NOT NULL,
    CancelBy        NVARCHAR(20)  NOT NULL CHECK (CancelBy IN ('CUSTOMER','COMPANY')),
    CancelReason    NVARCHAR(500),
    RefundPercent   DECIMAL(5,2)  NOT NULL CHECK (RefundPercent BETWEEN 0 AND 100),
    /* FIX: RefundAmount > 0 thực tế (chỉ tạo bản ghi khi có tiền hoàn) */
    RefundAmount    DECIMAL(12,2) NOT NULL CHECK (RefundAmount >= 0),
    CreatedAt       DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_BookingCancellations_Bookings FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID)
);
 
-- Mỗi đơn hàng chỉ hủy 1 lần
CREATE UNIQUE INDEX UQ_BookingCancellations_BookingID ON BOOKING_CANCELLATIONS(BookingID);
 
-- Lưu trữ nhật ký trả tiền cho khách khi đơn hàng bị hủy
CREATE TABLE REFUNDS
(
    RefundID      INT           PRIMARY KEY IDENTITY(1,1),
    BookingID     INT           NOT NULL,
    /* FIX: RefundAmount > 0 – không tạo bản ghi hoàn tiền khi số tiền = 0 */
    RefundAmount  DECIMAL(12,2) NOT NULL CHECK (RefundAmount > 0),
    RefundMethod  NVARCHAR(50)  NOT NULL,
    RefundStatus  NVARCHAR(20)  NOT NULL CHECK (RefundStatus IN ('PENDING','SUCCESS','FAILED')),
    TransactionID NVARCHAR(100) NULL,
    RefundDate    DATETIME      NOT NULL DEFAULT GETDATE(),
    CreatedAt     DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_Refunds_Bookings FOREIGN KEY (BookingID) REFERENCES BOOKINGS(BookingID)
);
 
-- Mỗi booking chỉ có tối đa 1 lần hoàn tiền đang PENDING
CREATE UNIQUE INDEX UIX_Refunds_PendingBooking ON REFUNDS(BookingID) WHERE RefundStatus='PENDING';
 
/* ============================================================
   5. PHÂN HỆ HẠ TẦNG KỸ THUẬT
   ============================================================ */
 
-- Theo dõi số lần một user đã dùng một coupon (chống dùng quá lượt)
CREATE TABLE USER_COUPON_USAGE
(
    UserID     INT      NOT NULL,
    CouponID   INT      NOT NULL,
    UsageCount INT      NOT NULL DEFAULT 0 CHECK (UsageCount >= 0),
    LastUsedAt DATETIME NULL,
    PRIMARY KEY (UserID, CouponID),
    CONSTRAINT FK_UserCouponUsage_Users   FOREIGN KEY (UserID)   REFERENCES USERS(UserID),
    CONSTRAINT FK_UserCouponUsage_Coupons FOREIGN KEY (CouponID) REFERENCES COUPONS(CouponID)
);
 
-- Nhật ký giám sát hệ thống (ai sửa trạng thái đơn hàng, lúc nào)
CREATE TABLE AUDIT_LOG
(
    AuditID     INT           PRIMARY KEY IDENTITY(1,1),
    TableName   NVARCHAR(50)  NOT NULL,
    RecordID    INT           NOT NULL,
    AuditAction NVARCHAR(20)  NOT NULL,
    OldStatus   NVARCHAR(20),
    NewStatus   NVARCHAR(20),
    ChangedByID INT           NULL,
    ChangedBy   NVARCHAR(100) NOT NULL DEFAULT SYSTEM_USER,
    ChangedAt   DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_AuditLog_Users FOREIGN KEY (ChangedByID) REFERENCES USERS(UserID)
);
 
-- Mã khóa giao dịch từ client, chống bấm đặt/thanh toán 2 lần thành 2 đơn
CREATE TABLE IDEMPOTENCY_KEYS
(
    IdempotencyKeyID BIGINT        PRIMARY KEY IDENTITY(1,1),
    IdempotencyKey   NVARCHAR(100) NOT NULL,
    OperationType    NVARCHAR(50)  NOT NULL,
    UserID           INT           NULL,
    RequestHash      NVARCHAR(128) NULL,
    Status           NVARCHAR(20)  NOT NULL CHECK (Status IN ('PROCESSING','SUCCESS','FAILED')),
    ResponseCode     NVARCHAR(50)  NULL,
    ResponseBody     NVARCHAR(MAX) NULL,
    ResourceType     NVARCHAR(50)  NULL,
    ResourceID       INT           NULL,
    ExpiresAt        DATETIME      NOT NULL DEFAULT DATEADD(DAY,1,GETDATE()),
    CreatedAt        DATETIME      NOT NULL DEFAULT GETDATE(),
    UpdatedAt        DATETIME      NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_IdempotencyKeys_Users      FOREIGN KEY (UserID) REFERENCES USERS(UserID),
    CONSTRAINT CHK_Idempotency_ExpiresAt_Future CHECK (ExpiresAt > CreatedAt)
);
 
CREATE UNIQUE INDEX UX_Idempotency_Operation_Key ON IDEMPOTENCY_KEYS(OperationType, IdempotencyKey);
 
-- Sự kiện hệ thống chờ đồng bộ ra các hệ thống khác (Outbox Pattern)
CREATE TABLE OUTBOX_EVENTS
(
    OutboxEventID BIGINT         PRIMARY KEY IDENTITY(1,1),
    EventType     NVARCHAR(100)  NOT NULL,
    AggregateType NVARCHAR(50)   NOT NULL,
    AggregateID   INT            NOT NULL,
    Payload       NVARCHAR(MAX)  NOT NULL,
    EventKey      NVARCHAR(120)  NULL,
    ActorID       INT            NULL,
    Status        NVARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (Status IN ('PENDING','PROCESSING','PUBLISHED','FAILED','DEAD')),
    RetryCount    INT            NOT NULL DEFAULT 0 CHECK (RetryCount >= 0),
    NextRetryAt   DATETIME       NULL,
    LastError     NVARCHAR(1000) NULL,
    CreatedAt     DATETIME       NOT NULL DEFAULT GETDATE(),
    PublishedAt   DATETIME       NULL,
    CONSTRAINT FK_OutboxEvents_Users FOREIGN KEY (ActorID) REFERENCES USERS(UserID),
    CONSTRAINT CHK_Outbox_Payload_IsJson CHECK (ISJSON(Payload)=1)
);
 
CREATE UNIQUE INDEX UX_Outbox_EventKey_NotNull ON OUTBOX_EVENTS(EventKey) WHERE EventKey IS NOT NULL;
 
-- Tác vụ chạy ngầm chờ xử lý (gửi email vé, thông báo tự động...)
CREATE TABLE BACKGROUND_JOBS
(
    JobID         BIGINT         PRIMARY KEY IDENTITY(1,1),
    JobType       NVARCHAR(80)   NOT NULL,
    Payload       NVARCHAR(MAX)  NOT NULL,
    Priority      TINYINT        NOT NULL DEFAULT 5 CHECK (Priority BETWEEN 1 AND 10),
    Status        NVARCHAR(20)   NOT NULL DEFAULT 'PENDING' CHECK (Status IN ('PENDING','RUNNING','DONE','FAILED','DEAD')),
    Attempts      INT            NOT NULL DEFAULT 0 CHECK (Attempts >= 0),
    MaxAttempts   INT            NOT NULL DEFAULT 10 CHECK (MaxAttempts BETWEEN 1 AND 100),
    NextRunAt     DATETIME       NOT NULL DEFAULT GETDATE(),
    TriggeredByID INT            NULL,
    LockedBy      NVARCHAR(100)  NULL,
    LockedAt      DATETIME       NULL,
    LastError     NVARCHAR(1000) NULL,
    CreatedAt     DATETIME       NOT NULL DEFAULT GETDATE(),
    UpdatedAt     DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_BackgroundJobs_Users FOREIGN KEY (TriggeredByID) REFERENCES USERS(UserID),
    CONSTRAINT CHK_Jobs_Payload_IsJson CHECK (ISJSON(Payload)=1)
);
 
-- Tham số cấu hình vận hành toàn hệ thống
CREATE TABLE SYSTEM_CONFIG
(
    ConfigKey   NVARCHAR(100)  PRIMARY KEY,
    ConfigValue NVARCHAR(1000) NOT NULL,
    ValueType   NVARCHAR(20)   NOT NULL CHECK (ValueType IN ('STRING','INT','DECIMAL','BOOL','JSON')),
    Description NVARCHAR(1000) NULL,
    IsActive    BIT            NOT NULL DEFAULT 1,
    UpdatedByID INT            NULL,
    UpdatedAt   DATETIME       NOT NULL DEFAULT GETDATE(),
    CONSTRAINT FK_SystemConfig_Users FOREIGN KEY (UpdatedByID) REFERENCES USERS(UserID),
    CONSTRAINT CHK_SystemConfig_ValueType CHECK (
        (ValueType='INT'     AND TRY_CAST(ConfigValue AS INT) IS NOT NULL)
        OR (ValueType='DECIMAL' AND TRY_CAST(ConfigValue AS DECIMAL(18,4)) IS NOT NULL)
        OR (ValueType='BOOL'    AND ConfigValue IN ('true','false','1','0'))
        OR (ValueType='JSON'    AND ISJSON(ConfigValue)=1)
        OR  ValueType='STRING'
    )
);
 
INSERT INTO SYSTEM_CONFIG (ConfigKey, ConfigValue, ValueType, Description) VALUES
('booking.max_pending_per_user',             '3',     'INT',  N'Giới hạn booking pending/paid cho mỗi user'),
('traffic.degraded_mode',                    'false', 'BOOL', N'Bật chế độ giảm tải hệ thống'),
('background_job.stuck_timeout_minutes',     '30',    'INT',  N'Số phút nhận diện job RUNNING bị treo'),
('payment.webhook.idempotency_ttl_minutes',  '1440',  'INT',  N'TTL idempotency key webhook');
GO
 
/* ============================================================
   6. INDEXES
   ============================================================ */
 
CREATE INDEX IDX_Tours_LocationID          ON TOURS(LocationID);
CREATE INDEX IDX_Tours_CategoryID          ON TOURS(CategoryID);
CREATE INDEX IDX_Tours_IsActive            ON TOURS(IsActive);
CREATE INDEX IDX_TourSchedules_TourID      ON TOUR_SCHEDULES(TourID);
CREATE INDEX IDX_TourSchedules_ScheduleDate ON TOUR_SCHEDULES(ScheduleDate);
CREATE INDEX IDX_Bookings_UserID           ON BOOKINGS(UserID);
CREATE INDEX IDX_Bookings_TourID           ON BOOKINGS(TourID);
CREATE INDEX IDX_Bookings_ScheduleID       ON BOOKINGS(ScheduleID, TourID);
CREATE INDEX IDX_Bookings_Status           ON BOOKINGS(Status);
CREATE INDEX IDX_Bookings_PendingUser      ON BOOKINGS(UserID) WHERE Status='PENDING';
CREATE INDEX IDX_BookingPassengers_BookingID ON BOOKING_PASSENGERS(BookingID);
CREATE INDEX IDX_Payments_BookingID        ON PAYMENTS(BookingID);
CREATE INDEX IDX_ETickets_TicketCode       ON E_TICKETS(TicketCode);
CREATE INDEX IDX_Reviews_BookingID         ON REVIEWS(BookingID);
CREATE INDEX IDX_FuelPrices_EffectiveDate  ON FUEL_PRICES(EffectiveDate);
CREATE INDEX IDX_TourPrices_TourID         ON TOUR_PRICES(TourID);
CREATE INDEX IDX_Audit_ChangedAt           ON AUDIT_LOG(ChangedAt);
CREATE INDEX IDX_Idempotency_Status_Expires ON IDEMPOTENCY_KEYS(Status, ExpiresAt);
CREATE INDEX IDX_Idempotency_Expires       ON IDEMPOTENCY_KEYS(ExpiresAt);
CREATE INDEX IDX_Outbox_Status_NextRetry   ON OUTBOX_EVENTS(Status, NextRetryAt, CreatedAt);
CREATE INDEX IDX_Outbox_Aggregate          ON OUTBOX_EVENTS(AggregateType, AggregateID, CreatedAt);
CREATE INDEX IDX_Outbox_Cleanup            ON OUTBOX_EVENTS(Status, PublishedAt) WHERE Status='PUBLISHED';
CREATE INDEX IDX_BackgroundJobs_Pick       ON BACKGROUND_JOBS(Status, Priority, NextRunAt, JobID);
GO
 
/* ============================================================
   7. TRIGGERS AN TOÀN DỮ LIỆU
   ============================================================ */
 
-- Tự động cập nhật BookedSlots khi danh sách hành khách thay đổi
CREATE OR ALTER TRIGGER trg_RecalcBookedSlots
ON BOOKING_PASSENGERS
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;
 
    ;WITH ChangedSchedules AS (
        SELECT DISTINCT b.ScheduleID
        FROM BOOKINGS b
        JOIN inserted i ON i.BookingID = b.BookingID
        UNION
        SELECT DISTINCT b.ScheduleID
        FROM BOOKINGS b
        JOIN deleted d ON d.BookingID = b.BookingID
    )
    UPDATE ts
    SET BookedSlots = x.TotalSlots
    FROM TOUR_SCHEDULES ts
    JOIN ChangedSchedules cs ON cs.ScheduleID = ts.ScheduleID
    CROSS APPLY (
        SELECT ISNULL(SUM(bp.SlotsOccupied), 0) AS TotalSlots
        FROM BOOKINGS b
        JOIN BOOKING_PASSENGERS bp ON bp.BookingID = b.BookingID
        WHERE b.ScheduleID = ts.ScheduleID
          AND b.Status <> 'CANCELLED'
    ) x;
 
    IF EXISTS (
        SELECT 1 FROM TOUR_SCHEDULES WHERE BookedSlots > AvailableSlots
    )
    BEGIN
        THROW 50001, N'Không đủ ghế trống cho lịch trình này.', 1;
    END
END;
GO
 
-- Kiểm tra số tiền thanh toán phải khớp FinalPrice
CREATE OR ALTER TRIGGER trg_ValidatePaymentAmount
ON PAYMENTS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
 
    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN BOOKINGS b ON b.BookingID = i.BookingID
        WHERE i.PaymentStatus = 'SUCCESS'
          AND ABS(i.Amount - b.FinalPrice) > 1   -- dung sai 1 đồng làm tròn
    )
    BEGIN
        THROW 50040, N'Số tiền thanh toán không khớp FinalPrice của booking.', 1;
    END
END;
GO
 
-- Tự động cập nhật Status của Booking theo kết quả thanh toán
CREATE OR ALTER TRIGGER trg_UpdateBookingStatus_OnPayment
ON PAYMENTS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
 
    UPDATE b
    SET b.Status = CASE i.PaymentStatus
        WHEN 'SUCCESS' THEN 'PAID'
        WHEN 'PENDING' THEN CASE WHEN b.Status = 'COMPLETED' THEN b.Status ELSE 'PENDING'   END
        WHEN 'FAILED'  THEN CASE WHEN b.Status = 'COMPLETED' THEN b.Status ELSE 'CANCELLED' END
        ELSE b.Status
    END
    FROM BOOKINGS b
    JOIN inserted i ON i.BookingID = b.BookingID
    WHERE b.Status <> 'CANCELLED';
END;
GO
 
-- Tự động phát hành E-Ticket khi thanh toán thành công
CREATE OR ALTER TRIGGER trg_CreateETicket_OnPaymentSuccess
ON PAYMENTS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;
 
    INSERT INTO E_TICKETS (BookingID, TicketCode, QRCode, TicketStatus, ExpiryDate)
    SELECT
        i.BookingID,
        CONCAT('TK_', FORMAT(GETDATE(),'yyyyMMdd'), '_', i.BookingID, '_',
               SUBSTRING(CONVERT(NVARCHAR(36), NEWID()), 1, 8)),
        CONCAT('QR_', i.BookingID, '_', FORMAT(GETDATE(),'yyyyMMddHHmmss')),
        'ACTIVE',
        DATEADD(DAY, t.Duration + 1, CAST(ts.ScheduleDate AS DATETIME))
    FROM inserted i
    JOIN BOOKINGS b      ON b.BookingID  = i.BookingID
    JOIN TOUR_SCHEDULES ts ON ts.ScheduleID = b.ScheduleID AND ts.TourID = b.TourID
    JOIN TOURS t           ON t.TourID     = b.TourID
    LEFT JOIN E_TICKETS et ON et.BookingID = i.BookingID
    WHERE i.PaymentStatus = 'SUCCESS'
      AND b.Status        <> 'CANCELLED'
      AND et.BookingID    IS NULL;          -- chưa có vé thì mới tạo
END;
GO
 
-- Ghi nhật ký mỗi khi Status của Booking thay đổi
CREATE OR ALTER TRIGGER trg_Audit_BookingStatus
ON BOOKINGS
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
 
    IF UPDATE(Status)
    BEGIN
        INSERT INTO AUDIT_LOG (TableName, RecordID, AuditAction, OldStatus, NewStatus)
        SELECT 'BOOKINGS', i.BookingID, 'STATUS_CHANGE', d.Status, i.Status
        FROM inserted i
        JOIN deleted d ON d.BookingID = i.BookingID
        WHERE i.Status <> d.Status;
    END
END;
GO
 
-- Tự động cập nhật UpdatedAt cho IDEMPOTENCY_KEYS
CREATE OR ALTER TRIGGER trg_Idempotency_UpdatedAt
ON IDEMPOTENCY_KEYS
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE k SET UpdatedAt = GETDATE()
    FROM IDEMPOTENCY_KEYS k
    JOIN inserted i ON i.IdempotencyKeyID = k.IdempotencyKeyID;
END;
GO
 
-- Tự động cập nhật UpdatedAt cho BACKGROUND_JOBS
CREATE OR ALTER TRIGGER trg_BackgroundJobs_UpdatedAt
ON BACKGROUND_JOBS
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE j SET UpdatedAt = GETDATE()
    FROM BACKGROUND_JOBS j
    JOIN inserted i ON i.JobID = j.JobID;
END;
GO
 
-- Tự động cập nhật UpdatedAt cho SYSTEM_CONFIG
CREATE OR ALTER TRIGGER trg_SystemConfig_UpdatedAt
ON SYSTEM_CONFIG
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    UPDATE c SET UpdatedAt = GETDATE()
    FROM SYSTEM_CONFIG c
    JOIN inserted i ON i.ConfigKey = c.ConfigKey;
END;
GO
 

-- Không cho sửa dữ liệu lõi của booking sau khi tạo
CREATE OR ALTER TRIGGER trg_Bookings_Guard_Update
ON BOOKINGS
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.BookingID = i.BookingID
        WHERE i.UserID <> d.UserID
           OR i.TourID <> d.TourID
           OR i.ScheduleID <> d.ScheduleID
    )
    BEGIN
        THROW 50101, N'Không được sửa UserID, TourID hoặc ScheduleID của booking.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.BookingID = i.BookingID
        WHERE d.Status IN ('PAID','COMPLETED')
          AND (i.TotalPrice <> d.TotalPrice
               OR i.DiscountAmount <> d.DiscountAmount
               OR i.SurchargeAmount <> d.SurchargeAmount)
    )
    BEGIN
        THROW 50102, N'Không thể thay đổi giá sau khi booking đã thanh toán hoặc hoàn thành.', 1;
    END;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN deleted d ON d.BookingID = i.BookingID
        WHERE i.Status = 'COMPLETED'
          AND d.Status <> 'PAID'
    )
    BEGIN
        THROW 50103, N'Chỉ booking PAID mới được chuyển sang COMPLETED.', 1;
    END;
END;
GO

-- Khi booking bị huỷ, tự tính lại số ghế đã đặt của lịch trình
CREATE OR ALTER TRIGGER trg_UpdateBookedSlots_OnBookingCancel
ON BOOKINGS
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(Status) RETURN;

    ;WITH ChangedSchedules AS (
        SELECT DISTINCT i.ScheduleID
        FROM inserted i
        JOIN deleted d ON d.BookingID = i.BookingID
        WHERE d.Status <> 'CANCELLED'
          AND i.Status = 'CANCELLED'
    )
    UPDATE ts
    SET BookedSlots = x.TotalSlots
    FROM TOUR_SCHEDULES ts
    JOIN ChangedSchedules cs ON cs.ScheduleID = ts.ScheduleID
    CROSS APPLY (
        SELECT ISNULL(SUM(bp.SlotsOccupied), 0) AS TotalSlots
        FROM BOOKINGS b
        JOIN BOOKING_PASSENGERS bp ON bp.BookingID = b.BookingID
        WHERE b.ScheduleID = ts.ScheduleID
          AND b.Status <> 'CANCELLED'
    ) x;
END;
GO

-- Khi booking bị huỷ, vé điện tử sẽ hết hiệu lực
CREATE OR ALTER TRIGGER trg_DisableETicket_OnBookingCancel
ON BOOKINGS
AFTER UPDATE
AS
BEGIN
    SET NOCOUNT ON;
    IF NOT UPDATE(Status) RETURN;

    UPDATE et
    SET TicketStatus = 'EXPIRED'
    FROM E_TICKETS et
    JOIN inserted i ON i.BookingID = et.BookingID
    JOIN deleted d ON d.BookingID = i.BookingID
    WHERE i.Status = 'CANCELLED'
      AND d.Status <> 'CANCELLED'
      AND et.TicketStatus = 'ACTIVE';
END;
GO

-- Dịch vụ mua thêm chỉ được sửa khi booking chưa thanh toán, chưa hoàn thành, chưa huỷ
CREATE OR ALTER TRIGGER trg_Addons_UpdateTotalPrice
ON BOOKING_ADD_ONS
AFTER INSERT, UPDATE, DELETE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM (
            SELECT BookingID FROM inserted
            UNION
            SELECT BookingID FROM deleted
        ) x
        JOIN BOOKINGS b ON b.BookingID = x.BookingID
        WHERE b.Status IN ('PAID','COMPLETED','CANCELLED')
    )
    BEGIN
        THROW 50053, N'Không thể sửa add-on khi booking đã thanh toán, hoàn thành hoặc đã huỷ.', 1;
    END;

    ;WITH Delta AS (
        SELECT ISNULL(i.BookingID, d.BookingID) AS BookingID,
               SUM(ISNULL(i.Price * i.Quantity, 0) - ISNULL(d.Price * d.Quantity, 0)) AS DeltaAmount
        FROM inserted i
        FULL JOIN deleted d ON d.BookingAddOnID = i.BookingAddOnID
        GROUP BY ISNULL(i.BookingID, d.BookingID)
    )
    UPDATE b
    SET TotalPrice = CASE
        WHEN b.TotalPrice + d.DeltaAmount < 0 THEN 0
        ELSE b.TotalPrice + d.DeltaAmount
    END,
    DiscountAmount = CASE
        WHEN b.DiscountAmount > CASE WHEN b.TotalPrice + d.DeltaAmount < 0 THEN 0 ELSE b.TotalPrice + d.DeltaAmount END
        THEN CASE WHEN b.TotalPrice + d.DeltaAmount < 0 THEN 0 ELSE b.TotalPrice + d.DeltaAmount END
        ELSE b.DiscountAmount
    END
    FROM BOOKINGS b
    JOIN Delta d ON d.BookingID = b.BookingID;
END;
GO

-- Không cho thanh toán booking đã huỷ hoặc đã hoàn thành
CREATE OR ALTER TRIGGER trg_PreventPaymentForClosedBooking
ON PAYMENTS
AFTER INSERT, UPDATE
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN BOOKINGS b ON b.BookingID = i.BookingID
        WHERE b.Status IN ('CANCELLED','COMPLETED')
          AND i.PaymentStatus <> 'FAILED'
    )
    BEGIN
        THROW 50041, N'Không thể thanh toán cho booking đã huỷ hoặc đã hoàn thành.', 1;
    END;
END;
GO

-- Khi ghi nhận huỷ booking, tự chuyển booking sang CANCELLED
CREATE OR ALTER TRIGGER trg_SyncBookingCancellation
ON BOOKING_CANCELLATIONS
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN BOOKINGS b ON b.BookingID = i.BookingID
        WHERE i.RefundAmount > b.FinalPrice
    )
    BEGIN
        THROW 50044, N'Số tiền hoàn không được lớn hơn FinalPrice của booking.', 1;
    END;

    UPDATE b
    SET Status = 'CANCELLED'
    FROM BOOKINGS b
    JOIN inserted i ON i.BookingID = b.BookingID
    WHERE b.Status NOT IN ('CANCELLED','COMPLETED');
END;
GO

-- Khi công ty huỷ lịch trình, tự tạo bản ghi huỷ cho các booking đang chờ/thanh toán
CREATE OR ALTER TRIGGER trg_HandleTourCancellation
ON TOUR_CANCELLATIONS
AFTER INSERT
AS
BEGIN
    SET NOCOUNT ON;

    INSERT INTO BOOKING_CANCELLATIONS (BookingID, CancelBy, CancelReason, RefundPercent, RefundAmount)
    SELECT b.BookingID,
           'COMPANY',
           CONCAT(N'Tour bị huỷ: ', i.CancelReason),
           ISNULL(i.RefundPercent, 100),
           b.FinalPrice * ISNULL(i.RefundPercent, 100) / 100.0
    FROM inserted i
    JOIN BOOKINGS b ON b.ScheduleID = i.ScheduleID
    LEFT JOIN BOOKING_CANCELLATIONS bc ON bc.BookingID = b.BookingID
    WHERE b.Status IN ('PENDING','PAID')
      AND i.ResolutionType = 'REFUND'
      AND bc.BookingID IS NULL;
END;
GO

-- Chỉ cho review booking thuộc đúng user và đã COMPLETED
CREATE OR ALTER TRIGGER trg_Reviews_Guard
ON REVIEWS
INSTEAD OF INSERT
AS
BEGIN
    SET NOCOUNT ON;

    IF EXISTS (
        SELECT 1
        FROM inserted i
        JOIN BOOKINGS b ON b.BookingID = i.BookingID
        WHERE b.UserID <> i.UserID
           OR b.Status <> 'COMPLETED'
    )
    BEGIN
        THROW 50121, N'Chỉ được đánh giá booking đã COMPLETED và thuộc đúng user.', 1;
    END;

    INSERT INTO REVIEWS (UserID, BookingID, Rating, ReviewContent, ReviewDate, CreatedAt)
    SELECT UserID, BookingID, Rating, ReviewContent,
           ISNULL(ReviewDate, GETDATE()),
           ISNULL(CreatedAt, GETDATE())
    FROM inserted;
END;
GO

-- Đặt thứ tự trigger thanh toán: kiểm tra tiền trước, rồi mới cập nhật trạng thái/tạo vé
EXEC sp_settriggerorder @triggername='trg_ValidatePaymentAmount', @order='First', @stmttype='INSERT';
EXEC sp_settriggerorder @triggername='trg_ValidatePaymentAmount', @order='First', @stmttype='UPDATE';
GO

/* ============================================================
   8. STORED PROCEDURES
   ============================================================ */
 
-- Tính giá tour cho Java trước khi tạo Booking
-- Quy tắc giá vé: ADULT=100%, CHILD=50%, BABY=25%
CREATE OR ALTER PROCEDURE sp_GetTourPrice
    @TourID     INT,
    @ScheduleDate DATE,
    @AdultCount INT = 1,
    @ChildCount INT = 0,
    @BabyCount  INT = 0
AS
BEGIN
    SET NOCOUNT ON;
 
    IF ISNULL(@AdultCount,0) < 0 OR ISNULL(@ChildCount,0) < 0 OR ISNULL(@BabyCount,0) < 0
        THROW 50050, N'Số lượng hành khách không được âm.', 1;
 
    IF ISNULL(@AdultCount,0) + ISNULL(@ChildCount,0) + ISNULL(@BabyCount,0) = 0
        THROW 50051, N'Phải có ít nhất 1 hành khách.', 1;
 
    DECLARE @BasePrice   DECIMAL(12,2),
            @Multiplier  DECIMAL(4,2),
            @Surcharge   DECIMAL(12,2),
            @FinalPrice  DECIMAL(12,2);
 
    -- Lấy giá hiệu lực gần nhất (TOUR_PRICES), fallback về BasePrice trong TOURS
    SELECT @BasePrice = ISNULL(tp.Price, t.BasePrice)
    FROM TOURS t
    OUTER APPLY (
        SELECT TOP 1 Price
        FROM TOUR_PRICES
        WHERE TourID = @TourID AND EffectiveDate <= @ScheduleDate
        ORDER BY EffectiveDate DESC
    ) tp
    WHERE t.TourID = @TourID AND t.IsActive = 1;
 
    SELECT @Multiplier = PriceMultiplier,
           @Surcharge  = Surcharge
    FROM TOUR_SCHEDULES
    WHERE TourID = @TourID AND ScheduleDate = @ScheduleDate;
 
    IF @BasePrice IS NULL OR @Multiplier IS NULL
        THROW 50052, N'Tour hoặc lịch khởi hành không hợp lệ / không còn hoạt động.', 1;
 
    SET @FinalPrice = (
          ISNULL(@AdultCount,0) * @BasePrice * 1.00
        + ISNULL(@ChildCount,0) * @BasePrice * 0.50
        + ISNULL(@BabyCount,0)  * @BasePrice * 0.25
    ) * @Multiplier + @Surcharge;
 
    SELECT
        @TourID       AS TourID,
        @ScheduleDate AS ScheduleDate,
        @BasePrice    AS BasePrice,
        @Multiplier   AS PriceMultiplier,
        @Surcharge    AS Surcharge,
        @FinalPrice   AS FinalPrice,
        @AdultCount   AS AdultCount,
        @ChildCount   AS ChildCount,
        @BabyCount    AS BabyCount;
END;
GO
 
-- Báo cáo doanh thu tour theo khoảng thời gian
CREATE OR ALTER PROCEDURE sp_GetTourRevenue
    @StartDate DATE,
    @EndDate   DATE
AS
BEGIN
    SET NOCOUNT ON;
 
    IF @StartDate > @EndDate
        THROW 50065, N'StartDate không được lớn hơn EndDate.', 1;
 
    ;WITH BookingAgg AS (
        SELECT TourID,
               COUNT(BookingID)    AS TotalBookings,
               SUM(FinalPrice)     AS TotalRevenue
        FROM BOOKINGS
        WHERE BookingDate >= @StartDate
          AND BookingDate <  DATEADD(DAY, 1, @EndDate)
          AND Status IN ('PAID','COMPLETED')
        GROUP BY TourID
    ),
    PaxAgg AS (
        SELECT b.TourID,
               COUNT(bp.PassengerID) AS TotalPassengers,
               SUM(bp.SlotsOccupied) AS TotalOccupiedSlots
        FROM BOOKINGS b
        JOIN BOOKING_PASSENGERS bp ON bp.BookingID = b.BookingID
        WHERE b.BookingDate >= @StartDate
          AND b.BookingDate <  DATEADD(DAY, 1, @EndDate)
          AND b.Status IN ('PAID','COMPLETED')
        GROUP BY b.TourID
    ),
    ReviewAgg AS (
        SELECT b.TourID,
               AVG(CAST(r.Rating AS DECIMAL(3,2))) AS AvgRating
        FROM BOOKINGS b
        JOIN REVIEWS r ON r.BookingID = b.BookingID
        WHERE b.BookingDate >= @StartDate
          AND b.BookingDate <  DATEADD(DAY, 1, @EndDate)
          AND b.Status IN ('PAID','COMPLETED')
        GROUP BY b.TourID
    )
    SELECT
        t.TourID,
        t.TourName,
        ISNULL(b.TotalBookings,   0) AS TotalBookings,
        ISNULL(p.TotalPassengers,    0) AS TotalPassengers,
        ISNULL(p.TotalOccupiedSlots, 0) AS TotalOccupiedSlots,
        ISNULL(b.TotalRevenue,      0) AS TotalRevenue,
        r.AvgRating
    FROM TOURS t
    LEFT JOIN BookingAgg  b ON b.TourID = t.TourID
    LEFT JOIN PaxAgg      p ON p.TourID = t.TourID
    LEFT JOIN ReviewAgg   r ON r.TourID = t.TourID
    ORDER BY TotalRevenue DESC;
END;
GO
 
-- Worker lấy 1 background job an toàn (tránh lỗi OUTPUT trực tiếp trên bảng có trigger)
CREATE OR ALTER PROCEDURE sp_ClaimNextBackgroundJob
    @WorkerName NVARCHAR(100)
AS
BEGIN
    SET NOCOUNT ON;
    SET XACT_ABORT ON;
 
    DECLARE @StuckTimeoutMinutes INT = 30;
 
    SELECT @StuckTimeoutMinutes = TRY_CAST(ConfigValue AS INT)
    FROM SYSTEM_CONFIG
    WHERE ConfigKey = 'background_job.stuck_timeout_minutes' AND IsActive = 1;
 
    SET @StuckTimeoutMinutes = ISNULL(@StuckTimeoutMinutes, 30);
 
    -- Giải phóng các job RUNNING bị treo quá lâu
    UPDATE BACKGROUND_JOBS
    SET Status    = CASE WHEN Attempts >= MaxAttempts THEN 'DEAD' ELSE 'PENDING' END,
        LockedBy  = NULL,
        LockedAt  = NULL,
        NextRunAt = DATEADD(MINUTE, POWER(2, CASE WHEN Attempts < 10 THEN Attempts ELSE 10 END), GETDATE()),
        LastError = CONCAT(N'Worker timeout sau ', @StuckTimeoutMinutes, N' phút')
    WHERE Status  = 'RUNNING'
      AND LockedAt < DATEADD(MINUTE, -@StuckTimeoutMinutes, GETDATE());
 
    -- Dùng bảng tạm để tránh lỗi OUTPUT trên bảng có trigger
    DECLARE @ClaimedJobs TABLE (
        JobID         BIGINT,
        JobType       NVARCHAR(80),
        Payload       NVARCHAR(MAX),
        Priority      TINYINT,
        Status        NVARCHAR(20),
        Attempts      INT,
        MaxAttempts   INT,
        NextRunAt     DATETIME,
        TriggeredByID INT,
        LockedBy      NVARCHAR(100),
        LockedAt      DATETIME,
        LastError     NVARCHAR(1000),
        CreatedAt     DATETIME,
        UpdatedAt     DATETIME
    );
 
    BEGIN TRANSACTION;
 
    ;WITH cte AS (
        SELECT TOP 1 JobID
        FROM BACKGROUND_JOBS WITH (UPDLOCK, READPAST, ROWLOCK)
        WHERE Status   = 'PENDING'
          AND NextRunAt <= GETDATE()
        ORDER BY Priority ASC, NextRunAt ASC, JobID ASC
    )
    UPDATE j
    SET Status   = 'RUNNING',
        LockedBy = @WorkerName,
        LockedAt = GETDATE(),
        Attempts = Attempts + 1
    OUTPUT inserted.JobID, inserted.JobType, inserted.Payload, inserted.Priority,
           inserted.Status, inserted.Attempts, inserted.MaxAttempts, inserted.NextRunAt,
           inserted.TriggeredByID, inserted.LockedBy, inserted.LockedAt,
           inserted.LastError, inserted.CreatedAt, inserted.UpdatedAt
    INTO @ClaimedJobs
    FROM BACKGROUND_JOBS j
    JOIN cte ON cte.JobID = j.JobID;
 
    COMMIT TRANSACTION;
 
    SELECT * FROM @ClaimedJobs;
END;
GO
 
-- Dọn dẹp các idempotency key đã hết hạn (chạy định kỳ)
CREATE OR ALTER PROCEDURE sp_CleanupExpiredIdempotencyKeys
AS
BEGIN
    SET NOCOUNT ON;
 
    DECLARE @Deleted INT = 1;
 
    WHILE @Deleted > 0
    BEGIN
        DELETE TOP (5000) FROM IDEMPOTENCY_KEYS
        WHERE ExpiresAt < GETDATE()
          AND Status    <> 'PROCESSING';
 
        SET @Deleted = @@ROWCOUNT;
    END
END;
GO
 

/* ============================================================
   9. VIEWS
   ============================================================ */

-- Tour nổi bật do nhân viên/trưởng phòng chọn để hiển thị trên trang chủ
CREATE OR ALTER VIEW vw_FeaturedTours
AS
SELECT
    ft.FeaturedTourID,
    ft.TourID,
    t.TourName,
    t.Description,
    t.BasePrice,
    t.Duration,
    t.Theme,
    l.LocationName,
    c.CategoryName,
    ft.DisplayOrder,
    ft.FeaturedTitle,
    ft.FeaturedDescription,
    ti.ImageURL
FROM FEATURED_TOURS ft
JOIN TOURS t 
    ON t.TourID = ft.TourID
JOIN LOCATIONS l 
    ON l.LocationID = t.LocationID
LEFT JOIN TOUR_CATEGORIES c 
    ON c.CategoryID = t.CategoryID
OUTER APPLY (
    SELECT TOP 1 ImageURL
    FROM TOUR_IMAGES ti
    WHERE ti.TourID = t.TourID
      AND ti.IsActive = 1
    ORDER BY ti.ImageID ASC
) ti
WHERE ft.IsActive = 1
  AND t.IsActive = 1
  AND (ft.StartDate IS NULL OR ft.StartDate <= CAST(GETDATE() AS DATE))
  AND (ft.EndDate IS NULL OR ft.EndDate >= CAST(GETDATE() AS DATE));
GO

-- Tour phổ biến / nhiều người đi, tính tự động từ BOOKINGS + BOOKING_PASSENGERS
CREATE OR ALTER VIEW vw_PopularTours
AS
SELECT
    t.TourID,
    t.TourName,
    t.Description,
    t.BasePrice,
    t.Duration,
    t.Theme,
    l.LocationName,
    c.CategoryName,
    COUNT(DISTINCT b.BookingID) AS TotalBookings,
    COUNT(bp.PassengerID) AS TotalPassengers,
    ISNULL(SUM(bp.SlotsOccupied), 0) AS TotalOccupiedSlots,
    ISNULL(SUM(b.FinalPrice), 0) AS TotalRevenue,
    ti.ImageURL
FROM TOURS t
JOIN LOCATIONS l 
    ON l.LocationID = t.LocationID
LEFT JOIN TOUR_CATEGORIES c 
    ON c.CategoryID = t.CategoryID
LEFT JOIN BOOKINGS b 
    ON b.TourID = t.TourID
   AND b.Status IN ('PAID','COMPLETED')
LEFT JOIN BOOKING_PASSENGERS bp 
    ON bp.BookingID = b.BookingID
OUTER APPLY (
    SELECT TOP 1 ImageURL
    FROM TOUR_IMAGES ti
    WHERE ti.TourID = t.TourID
      AND ti.IsActive = 1
    ORDER BY ti.ImageID ASC
) ti
WHERE t.IsActive = 1
GROUP BY
    t.TourID,
    t.TourName,
    t.Description,
    t.BasePrice,
    t.Duration,
    t.Theme,
    l.LocationName,
    c.CategoryName,
    ti.ImageURL;
GO

/* ============================================================
   10. VERIFY SAU KHI CHẠY SCRIPT
   ============================================================ */
 
-- Danh sách bảng
SELECT TABLE_NAME
FROM INFORMATION_SCHEMA.TABLES
WHERE TABLE_TYPE = 'BASE TABLE'
ORDER BY TABLE_NAME;
GO
 
-- Kiểm tra tour + địa điểm + danh mục
SELECT
    t.TourID,
    t.TourName,
    l.LocationName,
    c.CategoryName,
    t.BasePrice,
    t.Duration,
    t.Theme
FROM TOURS t
JOIN  LOCATIONS       l ON l.LocationID  = t.LocationID
LEFT JOIN TOUR_CATEGORIES c ON c.CategoryID = t.CategoryID
ORDER BY t.TourID;
GO
 
-- Kiểm tra lịch trình
SELECT
    ts.ScheduleID,
    t.TourName,
    ts.ScheduleDate,
    ts.AvailableSlots,
    ts.BookedSlots,
    ts.PriceMultiplier,
    ts.Surcharge
FROM TOUR_SCHEDULES ts
JOIN TOURS t ON t.TourID = ts.TourID
ORDER BY ts.ScheduleDate;
GO
 
-- Test stored procedure tính giá
EXEC sp_GetTourPrice @TourID=1, @ScheduleDate='2026-07-10', @AdultCount=2, @ChildCount=1, @BabyCount=1;
GO
 

-- Kiểm tra view tour nổi bật
SELECT *
FROM vw_FeaturedTours
ORDER BY DisplayOrder ASC;
GO

-- Kiểm tra view tour phổ biến
SELECT TOP 6 *
FROM vw_PopularTours
ORDER BY TotalPassengers DESC, TotalBookings DESC;
GO
