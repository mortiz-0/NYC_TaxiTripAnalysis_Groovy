import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVParser

def file = new File("NYC_Yellow_Taxi_Trip_Data.csv")
file.withReader { reader ->
    def csvParser = new CSVParser(reader, CSVFormat.DEFAULT.withFirstRecordAsHeader())

    for (record in csvParser) {
        def fare = record.get("total_amount")
        def passengers = record.get("passenger_count")
        // Your logic here...
    }
}
