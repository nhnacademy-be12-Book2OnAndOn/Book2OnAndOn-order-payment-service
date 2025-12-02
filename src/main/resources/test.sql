INSERT INTO Orders (
    user_id,
    order_number,
    order_datatime,  -- 엔티티의 @Column(name="order_datatime")과 일치시킴
    order_status,
    total_amount,
    total_discount_amount,
    total_item_amount,
    delivery_fee,
    wrapping_fee,
    coupon_discount,
    point_discount
) VALUES (
             1,
             'B20000000009',       -- HTML의 orderId와 일치해야 함
             '2025-01-01 10:10:10', -- 날짜 형식
             1,                    -- OrderStatus (Enum 매핑에 따라 다를 수 있음, 보통 0 또는 1)
             20000,                -- HTML의 amount와 일치해야 함
             0,
             10000,
             0,
             0,
             0,
             0
         ),
      (
       1,
       '1',
       '2025-01-01 10:10:10',
       1,
       20000,
       0,
       0,
       0,
       0,
       0,
       0
      );