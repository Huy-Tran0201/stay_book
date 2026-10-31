# StayBook entity relationship diagram

```mermaid
erDiagram
    USER ||--o{ BOOKING : places
    USER ||--o{ ROOM_BLOCK : creates
    USER ||--o{ PAYMENT : records
    USER ||--o{ SURCHARGE : records
    USER ||--o{ REVIEW : writes
    USER ||--o{ AUDIT_LOG : performs
    USER ||--o{ PASSWORD_RESET_TOKEN : owns
    ROOM_TYPE ||--o{ ROOM : contains
    ROOM_TYPE ||--o{ ROOM_TYPE_IMAGE : has
    ROOM_TYPE ||--o{ SEASON_PRICE : prices
    ROOM_TYPE }o--o{ AMENITY : offers
    ROOM ||--o{ ROOM_BLOCK : has
    ROOM ||--o{ BOOKING : assigned
    ROOM_TYPE ||--o{ BOOKING : requested
    BOOKING ||--o{ BOOKING_NIGHT : includes
    BOOKING ||--o{ BOOKING_EVENT : records
    BOOKING ||--o{ PAYMENT : receives
    BOOKING ||--o{ SURCHARGE : adds
    BOOKING ||--o| REVIEW : reviewed

    USER {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR role
        VARCHAR status
    }
    ROOM_TYPE {
        BIGINT id PK
        VARCHAR name
        BIGINT base_price
        VARCHAR status
    }
    ROOM {
        BIGINT id PK
        VARCHAR room_number UK
        BIGINT room_type_id FK
        VARCHAR status
    }
    BOOKING {
        BIGINT id PK
        VARCHAR code UK
        BIGINT room_id FK
        DATE check_in_date
        DATE check_out_date
        VARCHAR status
    }
    BOOKING_NIGHT {
        BIGINT id PK
        BIGINT booking_id FK
        DATE night_date
        BIGINT price
    }
```

Khoảng ngày đặt phòng dùng nửa mở: hai booking chồng lấn khi
`a.checkIn < b.checkOut && b.checkIn < a.checkOut`; vì vậy trả ngày 8 và nhận ngày 8 là liền kề, không phải chồng lấn.
