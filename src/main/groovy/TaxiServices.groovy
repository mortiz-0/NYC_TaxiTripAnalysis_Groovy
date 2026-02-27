import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.text.SimpleDateFormat

class TaxiServices{
    int total = 0
    Map<Integer, Map> byPassengerCount = [:]
    // Calculate average
    static Map getMapAvgValue(CSVRecord record, Map current, Double amount, String avg_attribute){
        current[avg_attribute] = ((current[avg_attribute]*current["size"])+ amount)/(current["size"]+1)
        return current
    }
    static Double trip_duration(String date_i,String date_f){
        // Convert datetime into trip duration
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss")
        Date date_in = dateFormat.parse(date_i)
        Date date_fi = dateFormat.parse(date_f)
        return (date_fi.time - date_in.time) / (1000 * 60)
    }
    // Process each record for average info for each function
    void f1_get_passenger_count(CSVRecord record){
        //Function to get average trip info by passenger count
        int current_key = record.get("passenger_count")?.isInteger() ? record.get("passenger_count").toInteger() : 0
        Map current_value = byPassengerCount.get(Integer.parseInt(record.get("passenger_count")))
        if ( current_value == null) {
            double price = Double.parseDouble(record.get("total_amount").toString())
            double duration = trip_duration(record.get("pickup_datetime"), record.get("dropoff_datetime"))
            double toll_amount = Double.parseDouble(record.get("tolls_amount").toString())
            Map<String, Integer> payment_methods = [(record.get("payment_type").toString()):1]
            Map value_map = ["size": 1, "avg_price": price, "avg_duration": duration, "avg_tolls": toll_amount, "payment_methods": payment_methods]
            byPassengerCount[current_key] = value_map
            total +=1
        }
        else {
            double price = Double.parseDouble(record.get("total_amount").toString())
            double duration = trip_duration(record.get("pickup_datetime"), record.get("dropoff_datetime"))
            double toll_amount = Double.parseDouble(record.get("tolls_amount").toString())
            if (!current_value["payment_methods"].containsKey(record.get("payment_type").toString())){
                current_value["payment_methods"][record.get("payment_type").toString()] = 1
            }
            Map new_value_map = getMapAvgValue(record, current_value, price, "avg_price")
            new_value_map = getMapAvgValue(record, new_value_map, duration, "avg_duration")
            new_value_map = getMapAvgValue(record, new_value_map, toll_amount, "avg_tolls")
            new_value_map["size"] = new_value_map["size"] + 1
            total +=1
            byPassengerCount[current_key] = new_value_map
        }
    }
    // Load CSV file and process records
    void loadCSV(String filePath){
        long startTime = System.currentTimeMillis()
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
        long endTime = System.currentTimeMillis()
        println("Executed in ${endTime-startTime}ms.")
    }
    static printMapTable(Map map, String title, List<String> columns, List<String> valueKeys) {
        println("\n${title}:")

        def header = columns.collect { it.center(it.length() + 4) }.join(" | ")
        println("-" * header.length())
        println(header)
        println("-" * header.length())

        map.each { key, value ->
            def row = [key] + valueKeys.collect { value[it] }
            def formattedRow = []

            for (int i = 0; i < columns.size(); i++) {
                def cellValue = row[i]
                def columnWidth = columns[i].length() + 4

                if (cellValue instanceof Number) {
                    if (cellValue instanceof Double || cellValue instanceof Float) {
                        formattedRow << String.format("%.2f", cellValue).center(columnWidth)
                    } else {
                        formattedRow << cellValue.toString().center(columnWidth)
                    }
                } else {
                    formattedRow << cellValue.toString().center(columnWidth)
                }
            }
            println(formattedRow.join(" | "))
        }
        println("-" * header.length())
    }
    // Show function results
    void showF1Results(){
        List <String> columns = ["Passenger Count", "Size", "Avg. Price", "Avg. Duration", "Avg. Tolls"]
        List <String> valueKeys = ["size", "avg_price", "avg_duration", "avg_tolls"]
        printMapTable(byPassengerCount.sort{it.key}, "Average Trip Info by Passengers", columns, valueKeys)
        println("Total Trips Processed: " + total)
    }
}

