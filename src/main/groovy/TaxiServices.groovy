import groovy.json.JsonSlurper
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser
import org.apache.commons.csv.CSVRecord
import java.text.SimpleDateFormat
import groovy.json.JsonOutput

class TaxiServices{
    int total = 0
    static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd")
    Map<Integer, Map> byPassengerCount = [:]
    Map<String, Map> byPaymentMethod = [:]
    List trips = []
    //Calculate haversine
    static double haversine(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371 // Earth radius in kilometers
        double dLat = Math.toRadians(lat2 - lat1)
        double dLon = Math.toRadians(lon2 - lon1)
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
                        Math.sin(dLon / 2) * Math.sin(dLon / 2)
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
        return R * c
    }
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
    //Calculate neighborhoods taken from the JASON file to use JSONSlurper
    static String getNeighborhood(JsonSlurper jsonSlurper, File jsonFile,double latitude, double longitude) {
        def neighborhoods = jsonSlurper.parse(jsonFile)
        def neighborhood = neighborhoods.sort { n ->
            double nLat = n.location.latitude
            double nLon = n.location.longitude
            haversine(latitude, longitude, nLat, nLon)
        }.first()
        return neighborhood.neighborhood
    }
    static def getNeighborhoodPair(trip){
        def projectDir = new File(System.getProperty("user.dir"))
        def jsonFile = new File(projectDir, "../resources/data/neighborhoods.json")
        def jsonSlurper = new JsonSlurper()
        String neigh1 = getNeighborhood(jsonSlurper, jsonFile, trip.get("pickup_latitude").toDouble(), trip.get("pickup_longitude").toDouble())
        String neigh2 = getNeighborhood(jsonSlurper, jsonFile, trip.get("dropoff_latitude").toDouble(), trip.get("dropoff_longitude").toDouble())
        return ([neigh1, neigh2])
    }
    // Process each record for average info for each function
    @SuppressWarnings('SpellCheckingInspection')
    void f1_get_info(CSVRecord record, Map<Object,Map> map1,String attribute){
        long startTime = System.currentTimeMillis()
        boolean a = attribute=="passenger_count"
        //Condition info by passenger count or payment method
        String csv_attribute = attribute=="passenger_count" ? "payment_type" : "passenger_count"
        Map current_value = a? map1.get(Integer.parseInt(record.get(attribute))) : map1.get(record.get(attribute))
        def current_key = a? record.get(attribute)?.isInteger() ? record.get(attribute).toInteger() : 0 : record.get(attribute)
        Map value_map
        if ( current_value == null) {
            double price = Double.parseDouble(record.get("total_amount").toString())
            double duration = trip_duration(record.get("pickup_datetime"), record.get("dropoff_datetime"))
            double toll_amount = Double.parseDouble(record.get("tolls_amount").toString())
            Map<String, Integer> mapValue = [(record.get(csv_attribute).toString()):1]
            Map<String, Integer> date_value = [(record.get("pickup_datetime").substring(0,10)):1]
            value_map = ["size": 1, "avg_price": price, "avg_duration": duration, "avg_tolls": toll_amount
                         , (csv_attribute): mapValue, "pickup_datetime": date_value, "most_datetime": date_value ]
            map1[current_key] = value_map
            total +=1
        }
        else {
            double price = Double.parseDouble(record.get("total_amount").toString())
            double duration = trip_duration(record.get("pickup_datetime"), record.get("dropoff_datetime"))
            double toll_amount = Double.parseDouble(record.get("tolls_amount").toString())
            current_value[csv_attribute][record.get(csv_attribute)] = current_value[csv_attribute].get(record.get(csv_attribute).toString(), 0) + 1
            current_value["pickup_datetime"][record.get("pickup_datetime")] = current_value["pickup_datetime"].get(record.get("pickup_datetime").substring(0,10), 0) + 1
            current_value["most_datetime"] = current_value["pickup_datetime"].max{it.value}.key
            Map new_value_map = getMapAvgValue(record, current_value, price, "avg_price")
            new_value_map = getMapAvgValue(record, new_value_map, duration, "avg_duration")
            new_value_map = getMapAvgValue(record, new_value_map, toll_amount, "avg_tolls")
            new_value_map["size"] = new_value_map["size"] + 1
            total +=1
            map1[current_key] = new_value_map
        }
        long endTime = System.currentTimeMillis()
        //noinspection GroovyAssignabilityCheck
        map1["time"] = map1.get("time", 0) + (endTime-startTime)
    }

    static Map<String, Object> f2_get_info(List<Record> trips, Double fareI, Double fareF){
        long startTime = System.currentTimeMillis()
        //Filter trips by fare range
        List filteredTrips = trips.findAll {Double.parseDouble(it.get("fare_amount") as String)>= fareI && Double.parseDouble(it.get("fare_amount") as String)<= fareF}
        if (filteredTrips.size() == 0) {
            long endTime = System.currentTimeMillis()
            return ["size": 0, "time": endTime-startTime, "avgPrice": 0, "avgDuration": 0,
                    "avgDistance": 0, "avgTolls": 0, "mostPassengerCount": null,
                    "mostPaymentType": null, "mostDatetime": null]
        }
        //Calculate average info for filtered trips
        double avgPrice = filteredTrips.sum { it.get("total_amount").toDouble() } / filteredTrips.size()
        double avgDuration = filteredTrips.sum { //noinspection SpellCheckingInspection
            trip_duration(it.get("pickup_datetime") as String, it.get("dropoff_datetime") as String) } / filteredTrips.size()
        double avgDistance = filteredTrips.sum{it.get("trip_distance").toDouble()} / filteredTrips.size()
        double avgTolls = filteredTrips.sum { it.get("tolls_amount").toDouble() } / filteredTrips.size()
        def mostPassengerCount = filteredTrips.groupBy {it.get("passenger_count")}.max{it.value.size()}
        def mostPaymentType = filteredTrips.groupBy {it.get("payment_type")}.max{it.value.size()}
        // Group trips by date and find the most frequent date
        def mostDatetime = filteredTrips.groupBy {it.get("pickup_datetime").substring(0,10)}.max{it.value.size()}
        long endTime = System.currentTimeMillis()
        return ["size": filteredTrips.size(), "avgPrice": avgPrice, "avgDuration": avgDuration,
                "avgDistance": avgDistance, "avgTolls": avgTolls, "time": endTime-startTime,
                "mostPassengerCount": [mostPassengerCount.key, mostPassengerCount.value.size()],
                "mostPaymentType": [mostPaymentType.key, mostPaymentType.value.size()],
                "mostDatetime": [mostDatetime.key, mostDatetime.value.size()]]
    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    static Map<String, Object> f3_get_info(List<Record> trips, String filter,Date dateI, Date dateF){
        // Filter trips by date range
        long startTime = System.currentTimeMillis()
        List filteredTrips = trips.findAll {
            Date pickupDate = this.dateFormat.parse((String)it.get("pickup_datetime").substring(0,10))
            return pickupDate >= dateI && pickupDate <= dateF
        }
        if (filteredTrips.size() == 0) {
            long endTime = System.currentTimeMillis()
            return ["size": 0, "time": endTime-startTime, "mostNeighborhoodPair": null,
                    "avgTotalCost": 0, "avgDuration": 0, "avgDistance": 0]
        }
        def groupedFilteredTrips = filteredTrips.groupBy {getNeighborhoodPair(it)}
                .findAll {!(it.key[0]==(it.key[1]))}
        def mostNeighborhoodPair = filter==1?groupedFilteredTrips.max{it.value.sum {it.get("total_amount").toDouble()}/it.value.size()}
                :groupedFilteredTrips.min{it.value.sum {it.get("total_amount").toDouble()}/it.value.size()}
        double avgTotalCost = mostNeighborhoodPair.value.sum { it.get("total_amount").toDouble() } / mostNeighborhoodPair.value.size()
        double avgDuration = mostNeighborhoodPair.value.sum {
            trip_duration(it.get("pickup_datetime") as String, it.get("dropoff_datetime") as String) } / mostNeighborhoodPair.value.size()
        double avgDistance = mostNeighborhoodPair.value.sum{it.get("trip_distance").toDouble()} / mostNeighborhoodPair.value.size()
        long endTime = System.currentTimeMillis()
        return ["size": groupedFilteredTrips.size(), "time": endTime-startTime,
                "mostNeighborhoodPair": mostNeighborhoodPair.key, "avgTotalCost": avgTotalCost,
                "avgDuration": avgDuration, "avgDistance": avgDistance]
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
            if(filePath.find("taxi")) {
                def format = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .build()
                def csvParser = new CSVParser(reader, format)
                def records = csvParser.toList()
                int totalRecords = records.size()
                int count = 0
                int next_percent = 10
                for (record in records) {
                    f1_get_info(record, byPassengerCount, "passenger_count")
                    f1_get_info(record, byPaymentMethod, "payment_type")
                    trips.add(record)

                    count += 1
                    int current_percent = (int) (((count*100) / totalRecords))
                    if (current_percent >= next_percent) {
                        println("Processed ${next_percent}% (${count}/${totalRecords}) records...")
                        next_percent += 10
                    }
                }
            }
            //Load Neighborhoods CSV file and turn it into a JSON
            else if(filePath.contains("neighborhoods")) {
                def format = CSVFormat.DEFAULT.builder()
                        .setHeader()
                        .setSkipHeaderRecord(true)
                        .setDelimiter(';')
                        .build()
                def csvParser = new CSVParser(reader, format)
                def data= csvParser.collect{[neighborhood:it.get("neighborhood"),
                                   borough:it.get("borough"), location:[latitude: it.get("latitude").replace(',','.').toDouble()
                                    ,longitude:it.get("longitude").replace(',','.').toDouble()]]}
                def jsonOutput = JsonOutput.toJson(data)
                def jsonFile = new File(projectDir, "../resources/data/neighborhoods.json")
                jsonFile.withWriter("UTF-8") { writer -> writer.write(jsonOutput)}
                long endTime = System.currentTimeMillis()
                println("Executed in ${endTime-startTime}ms.")
            }
        }
    }

    static void printMapTable(Map map, String title, List<String> columns, List<String> valueKeys) {
        println("\n${title}:")

        def columnWidths = [:]

        def firstColWidth = [columns[0].length(), map.keySet().max{it.toString().length()}?.toString()?.length() ?: 0].max()
        columnWidths[0] = firstColWidth + 4

        for (int i = 1; i < columns.size(); i++) {
            def headerWidth = columns[i].length()
            def maxValueWidth = 0

            //noinspection GroovyMissingReturnStatement
            map.each { key, value ->
                if (key != "time") {
                    def valueStr = value[valueKeys[i - 1]]?.toString() ?: ""
                    maxValueWidth = [maxValueWidth, valueStr.length()].max()
                }
            }

            columnWidths[i] = [headerWidth, maxValueWidth].max() + 4
        }

        def header = columns.collect { it.center(columnWidths[columns.indexOf(it)]) }.join(" | ")
        println("-" * header.length())
        println(header)
        println("-" * header.length())

        map.each { key, value ->
            if (key == "time") {
                return
            }
            def row = [key] + valueKeys.collect { value[it] }
            def formattedRow = []

            for (int i = 0; i < columns.size(); i++) {
                def cellValue = row[i]
                def columnWidth = columnWidths[i]

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
    void showF1Results(String option){
        def (mapKey, header) = option == "1" ? [byPassengerCount, "Average Trip Info by Passengers"] : [byPaymentMethod, "Average Trip Info by Payment Method"]
        def (columns, valueKeys) = option == "1" ?
                [["Passenger Count", "Size", "Avg. Price", "Avg. Duration", "Avg. Tolls", "Frequency of Payment Methods", "Most Frequent Datetime"],
                 ["size", "avg_price", "avg_duration", "avg_tolls", "payment_type", "most_datetime"]] :
                [["Payment Method", "Size", "Avg. Price", "Avg. Duration", "Avg. Tolls", "Frequency of Passengers Count", "Most Frequent Datetime"],
                 ["size", "avg_price", "avg_duration", "avg_tolls", "passenger_count", "most_datetime"]]

        printMapTable(mapKey.sort{it.key}, header, columns, valueKeys)
        println("Total Trips Processed: " + total/2)
        println("Executed in ${mapKey.get("time")}ms.")

    }
    void showF2Results(Double fareI, Double fareF){
        Map filteredTrips = f2_get_info(trips, fareI, fareF)
        String header = ("\nAverage Trip Info by Fare Range (${fareI} - ${fareF})")
        List <String> columns = ["Fare Range","Avg. Price", "Avg. Duration", "Avg. Distance", "Avg. Tolls", "Most Freq. Passenger Count", "Most Freq. Payment Type", "Most Frequent Date"]
        List <String> valueKeys= ["avgPrice", "avgDuration", "avgDistance", "avgTolls", "mostPassengerCount", "mostPaymentType", "mostDatetime"]
        //noinspection GroovyGStringKey
        printMapTable(["(${fareI}-${fareF})":filteredTrips], header, columns, valueKeys)
        println("Total Trips in Range: " + filteredTrips.size())
        println("Executed in ${filteredTrips.get("time")}ms.")

    }

    @SuppressWarnings('UnnecessaryQualifiedReference')
    void showF3Results(String filter, Date dateI, Date dateF){
        Map filteredTrips = f3_get_info(trips, filter, dateI, dateF)
        String header = ("\nNumber of Trips in Date Range (${this.dateFormat.format(dateI)} - ${this.dateFormat.format(dateF)}) ${filteredTrips.size()}")
            List <String> columns = ["Date Range", "Number of Trips", "Most Frequent Neighborhood Pair",
                                     "Avg. Total Cost", "Avg. Duration", "Avg. Distance"]
            List <String> valueKeys= ["size", "mostNeighborhoodPair", "avgTotalCost", "avgDuration", "avgDistance"]
            //noinspection GroovyGStringKey
            printMapTable(["(${this.dateFormat.format(dateI)} - ${this.dateFormat.format(dateF)})":filteredTrips], header, columns, valueKeys)
            println("Executed in ${filteredTrips.get("time")}ms.")
        }
}

