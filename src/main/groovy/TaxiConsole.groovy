class TaxiConsole {
    String[] functions = ["Load CSV", "Average Trip Info by Passengers", "Average Trip Info by Fare Range", "Exit"]

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

    void executeConsole() {
        // Initialize Scanner for user input and TaxiServices instance
        Scanner sc = new Scanner(System.in)
        TaxiServices taxiService = new TaxiServices()

        boolean working = true
        boolean loaded = false
        while (working) {
            printMenu()
            //Ensuring loading CSV first
            Integer input = null
            while (input == null) {
                input = getIntegerInput(sc, ("Select an option (0-" + (functions.length - 1).toString() + "): "))
                if (input == functions.length - 1 || input == 0) {
                    break
                }
                else if (loaded){
                    break
                }
                else {
                    input == ""
                    println("Please load the CSV file first.")
                }
            }
            switch (input) {
                case 0:
                    println("Loading CSV...")
                    loaded = true
                    taxiService.loadCSV("../resources/data/taxis-test.csv")
                    println("CSV Loaded Successfully.")

                    break
                case 1:
                    taxiService.showF1Results()
                    break
                case functions.length - 1:
                    println("Exiting...")
                    working = false
                    break
                default:
                    println("Invalid option. Please try again.")
                    break
            }
            getStringInput(sc, "Press any button to continue...")
        }
    }
}