import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord

class TaxiServices{
    int total = 0
    Map<Integer, Map> byPassengerCount = [:]
    // Calculate average
    Map getMapAvgValue(CSVRecord record, Map current, String record_attribute, String avg_attribute){
        double amount = Double.parseDouble(record.get(record_attribute).toString())
        current[avg_attribute] = ((current[avg_attribute]*current["size"])+ amount)/(current["size"]+1)
        current["size"] += 1
        total += 1
        return current
    }
    // Process each record for average info for each function
    void f1_get_passenger_count(CSVRecord record){
        //Function to get average trip info by passenger count
        int current_key = record.get("passenger_count")?.isInteger() ? record.get("passenger_count").toInteger() : 0
        Map current_value = byPassengerCount.get(Integer.parseInt(record.get("passenger_count")))
        if ( current_value == null) {
            double amount = Double.parseDouble(record.get("total_amount").toString())
            Map value_map = ["size": 1, "avg_price": amount]
            byPassengerCount[current_key] = value_map
        }
        else {
            Map new_value_map = getMapAvgValue(record, current_value, "total_amount", "avg_price")
            byPassengerCount[current_key] = new_value_map
        }
    }
    // Load CSV file and process records
    void loadCSV(String filePath){
        def projectDir = new File(System.getProperty("user.dir"))
        def file = new File(projectDir, filePath)

        if (!file.exists()) {
            throw new FileNotFoundException("El archivo no existe: ${file.absolutePath}")
        }

        file.withReader { reader ->
            def format = CSVFormat.DEFAULT.builder()
                    .setHeader()
                    .setSkipHeaderRecord(true)
                    .build()
            def csvParser = new CSVParser(reader, format)
            for (record in csvParser) {
                f1_get_passenger_count(record)
            }
        }
    }
    // Show function results
    void showF1Results(){
        println("\nAverage Trip Info by Passengers:")
        println("Passenger Count | Average Total Amount")
        println("---------------------------------------")
        byPassengerCount.each { key, value ->
            printf("%15d | %18.2f\n", key, value["avg_price"])
        }
        println("---------------------------------------")
        println("Total Trips Processed: " + total)
    }
}

