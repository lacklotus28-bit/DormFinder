#!/usr/bin/env python3
"""
Script to upload dormitory data from Excel to Firebase Firestore
Run this script to add the 39 dorms from the Excel file to your Firebase database
"""

import pandas as pd
import firebase_admin
from firebase_admin import credentials, firestore
import re
from datetime import datetime

# Initialize Firebase Admin SDK
# You'll need to download your service account key from Firebase Console
# Project Settings > Service Accounts > Generate New Private Key
cred = credentials.Certificate('C:\Users\Admin\Downloads\dormfinder-98225-firebase-adminsdk-fbsvc-73a57fdc3a.json')  # UPDATE THIS PATH
firebase_admin.initialize_app(cred)

db = firestore.client()

def clean_price(price_str):
    """Extract numeric price from string"""
    if pd.isna(price_str):
        return 0
    if isinstance(price_str, (int, float)):
        return float(price_str)
    
    # Extract first number from string
    match = re.search(r'\d+', str(price_str).replace(',', ''))
    return float(match.group()) if match else 0

def clean_phone(phone):
    """Format phone number"""
    if pd.isna(phone):
        return ""
    phone_str = str(phone).strip()
    # Remove any non-digit characters
    phone_str = re.sub(r'\D', '', phone_str)
    return phone_str

def convert_google_drive_urls(url_string):
    """Convert Google Drive share URLs to direct image URLs"""
    if pd.isna(url_string):
        return []
    
    urls = []
    # Split by comma
    url_list = str(url_string).split(',')
    
    for url in url_list:
        url = url.strip()
        # Extract file ID from Google Drive URL
        match = re.search(r'id=([a-zA-Z0-9_-]+)', url)
        if match:
            file_id = match.group(1)
            # Convert to direct image URL
            direct_url = f"https://drive.google.com/uc?export=view&id={file_id}"
            urls.append(direct_url)
        elif url:
            urls.append(url)
    
    return urls

def parse_amenities(amenities_str):
    """Parse amenities string into list"""
    if pd.isna(amenities_str):
        return []
    
    amenities = str(amenities_str).split(',')
    return [a.strip() for a in amenities if a.strip()]

def upload_dorms_to_firebase(excel_path):
    """Main function to upload dorms to Firebase"""
    
    # Read Excel file
    print(f"Reading Excel file: {excel_path}")
    df = pd.read_excel(excel_path)
    print(f"Found {len(df)} dormitories to upload\n")
    
    # Counter for successful uploads
    success_count = 0
    error_count = 0
    
    # Iterate through each row
    for index, row in df.iterrows():
        try:
            # Clean and prepare data
            dorm_name = row['Dormitory/Boarding House Name']
            if pd.isna(dorm_name) or str(dorm_name) == 'nan':
                # Generate a name from owner and location
                owner_name = row['Owner Full Name']
                address = str(row['Full Address of Property  ']).split(',')[0]
                dorm_name = f"{owner_name}'s Dormitory - {address}"
            else:
                dorm_name = str(dorm_name)
            
            # Create dormitory document
            dorm_data = {
                'name': dorm_name,
                'ownerName': str(row['Owner Full Name']),
                'ownerEmail': str(row['Owner Email Address']),
                'ownerPhone': clean_phone(row['Owner Contact Number  ']),
                'address': str(row['Full Address of Property  ']),
                'nearbyLandmark': str(row['Nearby School or Landmark  ']),
                'city': 'Batangas City',  # Default, you can extract from address if needed
                'totalRooms': int(row['Total Number of Rooms  ']) if not pd.isna(row['Total Number of Rooms  ']) else 1,
                'availableRooms': int(row['Total Number of Rooms  ']) if not pd.isna(row['Total Number of Rooms  ']) else 1,
                'roomType': str(row['Room Type  ']),
                'pricePerMonth': clean_price(row['Price per Month  ']),
                'amenities': parse_amenities(row['Amenities Offered  ']),
                'genderPreference': str(row['Gender Preference  ']),
                'rules': str(row['Dorm Rules or Notes  ']) if not pd.isna(row['Dorm Rules or Notes  ']) else '',
                'images': convert_google_drive_urls(row['Photo URL(s)  ']),
                'latitude': 13.7565,  # Default Batangas City coordinates
                'longitude': 121.0583,
                'rating': 0.0,
                'reviewCount': 0,
                'verified': False,
                'featured': False,
                'status': 'active',
                'createdAt': datetime.now(),
                'updatedAt': datetime.now()
            }
            
            # Add to Firestore
            doc_ref = db.collection('dormitories').add(dorm_data)
            
            print(f"✅ [{index + 1}/{len(df)}] Successfully uploaded: {dorm_name}")
            print(f"   Owner: {dorm_data['ownerName']}")
            print(f"   Location: {dorm_data['nearbyLandmark']}")
            print(f"   Price: ₱{dorm_data['pricePerMonth']}/month")
            print(f"   Document ID: {doc_ref[1].id}\n")
            
            success_count += 1
            
        except Exception as e:
            error_count += 1
            print(f"❌ [{index + 1}/{len(df)}] Error uploading row {index + 1}: {str(e)}\n")
            continue
    
    # Print summary
    print("\n" + "="*50)
    print("UPLOAD SUMMARY")
    print("="*50)
    print(f"✅ Successfully uploaded: {success_count} dormitories")
    print(f"❌ Failed uploads: {error_count} dormitories")
    print(f"📊 Total processed: {len(df)} dormitories")
    print("="*50)

if __name__ == "__main__":
    # Path to your Excel file
    excel_file = "/mnt/user-data/uploads/1761353446431_Dormitory_and_Boarding_House_Listing_Form__Responses_.xlsx"
    
    print("="*50)
    print("DORMITORY FIREBASE UPLOAD SCRIPT")
    print("="*50)
    print()
    
    # Upload dorms
    upload_dorms_to_firebase(excel_file)
    
    print("\n✨ Script completed!")
    print("\nNext steps:")
    print("1. Check Firebase Console to verify uploads")
    print("2. Update any dorm coordinates using Google Maps API")
    print("3. Add owner user accounts if needed")
