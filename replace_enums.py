import os
import glob

replacements = {
    "Promotion.DiscountType": "com.tomzxy.busozy.common.enums.DiscountType",
    "Booking.BookingStatus": "com.tomzxy.busozy.common.enums.BookingStatus",
    "Booking.PaymentStatus": "com.tomzxy.busozy.common.enums.BookingPaymentStatus",
    "Payment.PaymentStatus": "com.tomzxy.busozy.common.enums.PaymentTransactionStatus"
}

def replace_in_file(filepath):
    with open(filepath, 'r') as file:
        content = file.read()
    
    new_content = content
    for old, new in replacements.items():
        new_content = new_content.replace(old, new)
        
    if new_content != content:
        with open(filepath, 'w') as file:
            file.write(new_content)
        print(f"Updated {filepath}")

for root, _, files in os.walk("/home/tomzxy/projects/bussystem/src/main/java/com/tomzxy/busozy"):
    for file in files:
        if file.endswith(".java"):
            replace_in_file(os.path.join(root, file))
