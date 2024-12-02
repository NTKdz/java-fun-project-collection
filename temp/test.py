import cv2
import numpy as np
import matplotlib.pyplot as plt

# Load and preprocess image
image = cv2.imread('temp/brad_pit.jpg', cv2.IMREAD_GRAYSCALE)
blurred_image = cv2.GaussianBlur(image, (5, 5), 1.4)

# Sobel gradient calculation
sobel_x = cv2.Sobel(blurred_image, cv2.CV_64F, 1, 0, ksize=3)
sobel_y = cv2.Sobel(blurred_image, cv2.CV_64F, 0, 1, ksize=3)
magnitude = np.sqrt(sobel_x**2 + sobel_y**2)
theta = np.arctan2(sobel_y, sobel_x)

# Non-Maximum Suppression
def non_maximum_suppression(magnitude, theta):
    rows, cols = magnitude.shape
    suppressed_image = np.zeros_like(magnitude)

    for i in range(1, rows - 1):
        for j in range(1, cols - 1):
            angle = theta[i, j] * 180 / np.pi
            if angle < 0:
                angle += 180
            if (0 <= angle < 22.5) or (157.5 <= angle <= 180):
                neighbor1, neighbor2 = magnitude[i, j-1], magnitude[i, j+1]
            elif 22.5 <= angle < 67.5:
                neighbor1, neighbor2 = magnitude[i-1, j+1], magnitude[i+1, j-1]
            elif 67.5 <= angle < 112.5:
                neighbor1, neighbor2 = magnitude[i-1, j], magnitude[i+1, j]
            else:
                neighbor1, neighbor2 = magnitude[i-1, j-1], magnitude[i+1, j+1]
            if magnitude[i, j] >= neighbor1 and magnitude[i, j] >= neighbor2:
                suppressed_image[i, j] = magnitude[i, j]
            else:
                suppressed_image[i, j] = 0
    return suppressed_image

# Apply non-max suppression
suppressed_image = non_maximum_suppression(magnitude, theta)

# Double Thresholding
def double_thresholding(image, low_thresh, high_thresh):
    strong = 255
    weak = 75
    result = np.zeros_like(image)
    result[image >= high_thresh] = strong
    result[(image >= low_thresh) & (image < high_thresh)] = weak
    return result

low_threshold = 50
high_threshold = 150
thresholded_image = double_thresholding(suppressed_image, low_threshold, high_threshold)

# Hysteresis
def hysteresis(image):
    rows, cols = image.shape
    final_edges = image.copy()
    for i in range(1, rows - 1):
        for j in range(1, cols - 1):
            if image[i, j] == 75:  # weak pixel
                if np.any(image[i-1:i+2, j-1:j+2] == 255):
                    final_edges[i, j] = 255
                else:
                    final_edges[i, j] = 0
    return final_edges

# Apply hysteresis
final_edges = hysteresis(thresholded_image)

# Display images side by side
plt.figure(figsize=(12, 6))

# # Original image
# plt.subplot(2, 3, 1)
# plt.imshow(image, cmap='gray')
# plt.title("Original Image")
# plt.axis('off')

# # Blurred image
# plt.subplot(2, 3, 2)
# plt.imshow(blurred_image, cmap='gray')
# plt.title("Blurred Image")
# plt.axis('off')

# Gradient magnitude
plt.subplot(1, 2, 1)
plt.imshow(magnitude, cmap='gray')
plt.title("Gradient intensity")
plt.axis('off')

# Non-Maximum Suppression
plt.subplot(1, 2, 2)
plt.imshow(suppressed_image, cmap='gray')
plt.title("Non-Max Suppression")
plt.axis('off')

# # Thresholded Image (after double thresholding)
# plt.subplot(2, 3, 5)
# plt.imshow(thresholded_image, cmap='gray')
# plt.title("Double Thresholding")
# plt.axis('off')

# # Final Edges after Hysteresis
# plt.subplot(2, 3, 6)
# plt.imshow(final_edges, cmap='gray')
# plt.title("Final Edges (Hysteresis)")
# plt.axis('off')

plt.tight_layout()
plt.show()
