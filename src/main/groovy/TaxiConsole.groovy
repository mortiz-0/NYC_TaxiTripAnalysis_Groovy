import java.text.SimpleDateFormat

class TaxiConsole {
    String[] csvFiles = ["taxis-test.csv", "taxis-small.csv", "taxis-medium.csv", "taxis-large.csv"]
    String[] functions = ["Load CSV", "Average Trip Info by Passengers",
                          "Average Trip Info by Fare Range", "Average Trip Info by location and date range",
                          "Exit"]

    void printMenu() {
        println("\n---------------------" + "\nTaxi Trips Analysis" + "\n---------------------")
        for (int i = 0; i <= functions.length - 1; i++) {
            println(" " + i + ". " + functions[i])
        }
    }

    static String getStringInput(Scanner sc, String prompt) {
        println(prompt)
        return sc.nextLine()
    }

    Integer getIntegerInput(Scanner sc, String prompt) {
        println(prompt)
        try {
            return Integer.parseInt(sc.nextLine())
        } catch (NumberFormatException ignored) {
            println("Invalid input. Please enter a valid integer.")
            return getIntegerInput(sc, prompt)
        }
    }

    Double getDoubleInput(Scanner sc, String prompt) {
        println(prompt)
        try {
            return Double.parseDouble(sc.nextLine())
        } catch (NumberFormatException ignored) {
            println("Invalid input. Please enter a valid number.")
            return getDoubleInput(sc, prompt)
        }
    }

    Date getDateInput(Scanner sc, String prompt) {
        println(prompt)
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd")
        try {
            return dateFormat.parse(sc.nextLine())
        } catch (Exception ignored) {
            println("Invalid input. Please enter a valid date in the format 'yyyy-MM-dd'.")
            return getDateInput(sc, prompt)
        }
    }

    void executeConsole() {
        // Initialize Scanner for user input and TaxiServices instance
        Scanner sc = new Scanner(System.in)
        TaxiServices taxiService = new TaxiServices()

        boolean working = true
        boolean loaded = false
        while (working) {
            //Ensuring loading CSV first
            Integer input = null
            while (input == null) {
                printMenu()
                input = getIntegerInput(sc, ("Select an option (0-" + (functions.length - 1).toString() + "): "))
                if (input == functions.length - 1 || input == 0) {
                    break
                }
                else if (loaded){
                    break
                }
                else {
                    getStringInput(sc,"Please load the CSV file first.")
                }
                input = null
            }
            switch (input) {
                case 0:
                    println("Loading CSV...")
                    loaded = true
                    String file = null
                    while (file==null){
                        file = getStringInput(sc, "Select a csv to load:\n(1) taxis-test\n(2) taxis-small\n(3) taxis-medium\n(4) taxis-large")
                        if (!(["1", "2","3","4"].contains(file))){
                            println("Invalid option. Please try again.")
                            file = null
                            }
                        }
                    taxiService.loadCSV("../resources/data/${csvFiles[file.toInteger() - 1]}")
                    taxiService.loadCSV("../resources/data/nyc-neighborhoods.csv")
                    println("CSV Loaded Successfully.")
                    getStringInput(sc, "Press any button to continue...")
                    break
                case 1:
                    String method = null
                    while(method==null){
                        method = getStringInput(sc, "Select a filter, passenger count (1) or payment method (2):")
                        if (method != "1" && method != "2"){
                            println("Invalid option. Please try again.")
                            method = null
                        }
                    }
                    taxiService.showF1Results(method)
                    getStringInput(sc, "Press any button to continue...")
                    break
                case 2:
                    Double fareI = getDoubleInput(sc, "Enter the minimum fare amount:")
                    Double fareF = getDoubleInput(sc, "Enter the maximum fare amount:")
                    taxiService.showF2Results(fareI, fareF)
                    getStringInput(sc, "Press any button to continue...")
                    break
                case 3:
                    String filter = null
                    while(filter==null) {
                        filter = getStringInput(sc, "Select a filter, GREATER (1) or LESSER (2) average total cost:")
                        if (filter != "1" && filter != "2") {
                            println("Invalid option. Please try again.")
                            filter = null
                        }
                    }
                    Date dateI = getDateInput(sc, "Enter the start date (yyyy-MM-dd):")
                    Date dateF = getDateInput(sc, "Enter the end date (yyyy-MM-dd):")
                    taxiService.showF3Results(filter, dateI, dateF)
                    getStringInput(sc, "Press any button to continue...")
                    break
                case functions.length - 1:
                    println("Exiting...")
                    working = false
                    break
                default:
                    println("Invalid option. Please try again.")
                    getStringInput(sc, "Press any button to continue...")
                    break
            }
        }
    }
}