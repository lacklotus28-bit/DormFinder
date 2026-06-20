// Alternative upload method with better error handling
private void uploadImageWithRetry(Uri imageUri, int index, List<String> imageUrls, int retryCount) {
    if (retryCount > 3) {
        Toast.makeText(this, "Failed to upload image after 3 attempts", Toast.LENGTH_SHORT).show();
        resetSaveButton();
        return;
    }
    
    String fileName = "dorm_" + currentUserId + "_" + System.currentTimeMillis() + "_" + index + ".jpg";
    String imagePath = "dormitory_images/" + currentUserId + "/" + fileName;
    StorageReference imageRef = storageRef.child(imagePath);
    
    // Compress image before upload (optional)
    try {
        InputStream imageStream = getContentResolver().openInputStream(imageUri);
        if (imageStream != null) {
            imageRef.putStream(imageStream)
                .addOnSuccessListener(taskSnapshot -> {
                    imageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        imageUrls.add(uri.toString());
                        uploadNextImage(index + 1, imageUrls);
                    });
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("Upload Error", "Error uploading image: " + e.getMessage());
                    uploadImageWithRetry(imageUri, index, imageUrls, retryCount + 1);
                });
        }
    } catch (Exception e) {
        android.util.Log.e("Upload Error", "Error opening image stream: " + e.getMessage());
        Toast.makeText(this, "Error reading image file", Toast.LENGTH_SHORT).show();
        resetSaveButton();
    }
}
