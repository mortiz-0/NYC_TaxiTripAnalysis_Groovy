# NYC Taxi Trip Analysis (Groovy)

This is my first project developed using **Groovy**. It is a data analysis tool designed to process and extract insights from the "NYC Yellow Taxi Trip Data" dataset.

## 🚖 Project Overview
The goal of this project is to analyze a sample of 1000 taxi trip records from New York City (starting from 2015) to identify patterns, averages, and behaviors based on different filters such as passenger count, price ranges, and geographic locations.

### Dataset Information
*   **Source:** [NYC Yellow Taxi Trip Data (Kaggle)](https://www.kaggle.com/datasets/elemento/nyc-yellow-taxi-trip-data)
*   **Format:** Single CSV file (UTF-8)
*   **Content:** Information on trip duration, costs, passenger counts, payment types, and GPS coordinates.

---

## Implemented Functions

### F1 & F2: Average Trip Info by Passenger Count
Analyzes trip data filtered by a specific number of passengers (e.g., "1", "2", "3" or "5").
*   **Input:** Number of passengers to filter.
*   **Output:**
    *   Execution time in milliseconds.
    *   Total number of trips matching the filter.
    *   Average duration (minutes).
    *   Average total cost (USD).
    *   Average distance (miles).
    *   Average tolls paid.
    *   Most used payment method (Format: `METHOD - COUNT`).
    *   Average tip amount.
    *   Date with the highest trip frequency (Format: `%Y-%m-%d`).

### F3: Average Trip Info by Fare Range
Analyzes trips within a specific total fare range.
*   **Input:** Minimum price and Maximum price (e.g., "5" to "15.5").
*   **Output:**
    *   Execution time in milliseconds.
    *   Total number of trips within the range.
    *   Average duration (minutes).
    *   Average total price (USD).
    *   Average distance (miles).
    *   Average tolls paid.
    *   Most frequent passenger count (Format: `#PASSENGERS - COUNT`).
    *   Average tip amount.
    *   Most frequent trip end date (Format: `%Y-%m-%d`).

### F4: Extreme Cost Neighborhood Combinations
Identifies the origin-destination neighborhood pairs with the **highest** or **lowest** average total cost within a date range.
*   **Input:** Cost filter (`MAYOR` or `MENOR`), Start Date, and End Date (`%Y-%m-%d`).
*   **Logic:**
    *   Only considers trips where origin and destination are different.
    *   Uses the **Haversine formula** to map GPS coordinates to the nearest NYC neighborhood centroid.
*   **Output:**
    *   Execution time in milliseconds.
    *   Total trips within the date range.
    *   Origin and Destination neighborhood names.
    *   Average distance, duration, and total cost for that combination.

---
## 🛠️ Technical Details
* **Language:** Groovy 4.0.15
* **Build Tool:** Gradle 8.x
* **Runtime:** Java 21 (LTS)
* **Libraries:** [Apache Commons CSV](https://commons.apache.org/proper/commons-csv/) for high-performance data parsing.
* **Algorithms:** Haversine formula for geospatial distance calculation.

## 🔧 Setup & Execution
1.  **Prerequisites:** Ensure you have **Java 21** installed.
2.  **Clone the repo:**
    ```bash
    git clone [https://github.com/mortiz-0/NYC_TaxiTripAnalysis_Groovy.git](https://github.com/mortiz-0/NYC_TaxiTripAnalysis_Groovy.git)
    
    ```
3. **Run the analysis:**
    You can run the project using the Gradle wrapper without installing Gradle manually:
    * **Windows:** `gradlew.bat run`
    * **Mac/Linux:** `./gradlew run`
---
*Created as part of my first steps into Groovy development - 2026*
