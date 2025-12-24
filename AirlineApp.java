import java.io.*;
import java.nio.file.*;
import java.text.DecimalFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;
 
public class AirlineApp {

 
    public static final String RESET = "\u001B[0m";
    public static final String BLACK = "\u001B[30m";
    public static final String RED = "\u001B[31m";
    public static final String GREEN = "\u001B[32m";
    public static final String YELLOW = "\u001B[33m";
    public static final String BLUE = "\u001B[34m";
    public static final String PURPLE = "\u001B[35m";
    public static final String CYAN = "\u001B[36m";
    public static final String WHITE = "\u001B[37m";
    public static final String BOLD = "\u001B[1m";
    public static final String BG_BLUE = "\u001B[44m";
    public static final String BG_CYAN = "\u001B[46m";
    public static final String BG_GREEN = "\u001B[42m";
    public static final String BG_RED = "\u001B[41m";

    enum CabinClass {
        ECONOMY("Economy Class", 1.0, 4, 8),
        BUSINESS("Business Class", 2.5, 2, 4),
        FIRST("First Class", 5.0, 0, 2);

        final String label;
        final double multiplier;
        final int startRow, endRow;

        CabinClass(String l, double m, int s, int e) { label = l; multiplier = m; startRow = s; endRow = e; }
    }

    enum MealType {
        NONE("No Meal", 0),
        VEG("Standard Veg", 0),
        CHICKEN("Spicy Chicken", 450),
        JAIN("Jain Meal", 150),
        CHEF("Chef's Special", 1200);

        final String label;
        final double price;

        MealType(String l, double p) { label = l; price = p; }
    }

    static class DatabaseManager {
        private static final String FILE_USERS = "users.csv";
        private static final String FILE_BOOKINGS = "bookings.csv";
        private static final String FILE_FLIGHTS = "flights.csv"; // New in v7

        public static void initDB() {
            createFileIfNotExists(FILE_USERS, "username,password,name,isAdmin,wallet");
            createFileIfNotExists(FILE_BOOKINGS, "pnr,flightId,seat,owner,class,meal,price");
            createFileIfNotExists(FILE_FLIGHTS, "id,origin,destination,time,price");
            

            if(countLines(FILE_USERS) <= 1) {
                saveUser(new User("admin", "admin", "System Administrator", true));
                saveUser(new User("Aatreyee", "misra", "Aatreyee Misra", false));
            }

            if(countLines(FILE_FLIGHTS) <= 1) {
                seedFlights();
            }
        }

        private static void seedFlights() {
            List<Flight> seed = new ArrayList<>();
            seed.add(new Flight("AI-101", "DEL", "BOM", "08:00", 5500));
            seed.add(new Flight("6E-505", "BOM", "BLR", "10:30", 4200));
            seed.add(new Flight("UK-992", "DEL", "CCU", "14:15", 4800));
            seed.add(new Flight("SG-202", "MAA", "DEL", "06:00", 6100));
            seed.add(new Flight("QP-110", "BLR", "PNQ", "19:45", 3800));
            seed.add(new Flight("AI-440", "HYD", "DEL", "07:30", 5100));
            seed.add(new Flight("6E-202", "AMD", "GOI", "16:20", 3500));
            seed.add(new Flight("UK-818", "BLR", "BOM", "18:00", 4600));
            seed.add(new Flight("SG-707", "CCU", "IXB", "11:10", 2900));
            seed.add(new Flight("I5-320", "DEL", "SXR", "09:45", 6200));
            seed.add(new Flight("AI-881", "DEL", "IXL", "05:40", 8500));
            seed.add(new Flight("6E-636", "ATQ", "DEL", "20:15", 3100));
            
            for(Flight f : seed) saveFlight(f);
        }

        private static void createFileIfNotExists(String filename, String header) {
            File f = new File(filename);
            if(!f.exists()) {
                try (PrintWriter pw = new PrintWriter(new FileWriter(f))) {
                    pw.println(header);
                } catch (IOException e) { System.out.println(RED + "DB Init Error: " + e.getMessage() + RESET); }
            }
        }

        private static int countLines(String filename) {
            try { return (int) Files.lines(Paths.get(filename)).count(); } 
            catch (IOException e) { return 0; }
        }

        public static Map<String, User> loadUsers() {
            Map<String, User> users = new HashMap<>();
            try (BufferedReader br = new BufferedReader(new FileReader(FILE_USERS))) {
                String line; br.readLine(); // Skip header
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    if(data.length >= 5) {
                        User u = new User(data[0], data[1], data[2], Boolean.parseBoolean(data[3]));
                        u.wallet = Double.parseDouble(data[4]);
                        users.put(data[0], u);
                    }
                }
            } catch (IOException e) { }
            return users;
        }

        public static List<Flight> loadFlights() {
            List<Flight> flights = new ArrayList<>();
            try (BufferedReader br = new BufferedReader(new FileReader(FILE_FLIGHTS))) {
                String line; br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    if(data.length >= 5) {
                        flights.add(new Flight(data[0], data[1], data[2], data[3], Double.parseDouble(data[4])));
                    }
                }
            } catch (Exception e) {}
            return flights;
        }

        public static void loadBookings(List<Flight> flights, List<Ticket> tickets) {
            try (BufferedReader br = new BufferedReader(new FileReader(FILE_BOOKINGS))) {
                String line; br.readLine();
                while ((line = br.readLine()) != null) {
                    String[] data = line.split(",");
                    if(data.length >= 7) {
                        String flightId = data[1];
                        Flight f = flights.stream().filter(fl -> fl.id.equals(flightId)).findFirst().orElse(null);
                        if(f != null) {
                            String seat = data[2];
                            // Mark seat
                            try {
                                int r = Integer.parseInt(seat.substring(0, seat.length()-1)) - 1;
                                int c = seat.charAt(seat.length()-1) - 'A';
                                f.seats[r][c] = true;
                                tickets.add(new Ticket(f, seat, data[3], CabinClass.valueOf(data[4]), MealType.valueOf(data[5]), Double.parseDouble(data[6]), data[0]));
                            } catch (Exception e) { /* Ignore corrupt booking lines */ }
                        }
                    }
                }
            } catch (Exception e) {}
        }

        public static void saveUser(User u) {
            appendToCSV(FILE_USERS, String.join(",", u.username, u.pass, u.name, String.valueOf(u.isAdmin), String.valueOf(u.wallet)));
        }

        public static void saveFlight(Flight f) {
            appendToCSV(FILE_FLIGHTS, String.join(",", f.id, f.org, f.dst, f.time, String.valueOf(f.price)));
        }

        public static void saveBooking(Ticket t) {
            appendToCSV(FILE_BOOKINGS, String.join(",", t.bookingId, t.f.id, t.seat, t.owner, t.travelClass.name(), t.meal.name(), String.valueOf(t.paidPrice)));
        }

        private static void appendToCSV(String filename, String data) {
            try (PrintWriter pw = new PrintWriter(new FileWriter(filename, true))) {
                pw.println(data);
            } catch (IOException e) {}
        }

        public static void updateUserWallet(User u) {
            List<String> lines = readAllLines(FILE_USERS);
            for(int i=0; i<lines.size(); i++) {
                if(lines.get(i).startsWith(u.username + ",")) {
                    lines.set(i, String.join(",", u.username, u.pass, u.name, String.valueOf(u.isAdmin), String.valueOf(u.wallet)));
                }
            }
            writeAllLines(FILE_USERS, lines);
        }

        public static void deleteBooking(String pnr) {
            List<String> lines = readAllLines(FILE_BOOKINGS);
            lines.removeIf(line -> line.startsWith(pnr + ","));
            writeAllLines(FILE_BOOKINGS, lines);
        }

        public static void deleteFlight(String flightId) {
            List<String> lines = readAllLines(FILE_FLIGHTS);
            lines.removeIf(line -> line.startsWith(flightId + ","));
            writeAllLines(FILE_FLIGHTS, lines);
        }

        private static List<String> readAllLines(String file) {
            try { return Files.readAllLines(Paths.get(file)); } catch(Exception e) { return new ArrayList<>(); }
        }
        private static void writeAllLines(String file, List<String> lines) {
            try { Files.write(Paths.get(file), lines); } catch(Exception e) {}
        }
    }

    static class DataStore {
        static List<Flight> flights = new ArrayList<>();
        static List<Ticket> tickets = new ArrayList<>();
        static Map<String, User> users = new HashMap<>();
        static User currentUser = null;

        public static void refreshData() {
            users = DatabaseManager.loadUsers();
            flights = DatabaseManager.loadFlights(); // Now loaded from DB
            tickets.clear();
            DatabaseManager.loadBookings(flights, tickets);
        }
    }

    public static void main(String[] args) {
        showSplashScreen();
        DatabaseManager.initDB();
        DataStore.refreshData();
        randomizeSeats(); 

        Scanner scanner = new Scanner(System.in);
        while (true) {
            if (DataStore.currentUser == null) {
                authMenu(scanner);
            } else {
                if (DataStore.currentUser.isAdmin) adminMenu(scanner);
                else userMenu(scanner);
            }
        }
    }

    private static void showSplashScreen() {
        clearScreen();
        System.out.println(CYAN + "\n\n\n");
        System.out.println("          ███████╗██╗  ██╗██╗   ██╗ ██████╗ ██████╗ ███╗   ██╗███╗   ██╗███████╗ ██████╗████████╗");
        System.out.println("          ██╔════╝██║ ██╔╝╚██╗ ██╔╝██╔════╝██╔═══██╗████╗  ██║████╗  ██║██╔════╝██╔════╝╚══██╔══╝");
        System.out.println("          ███████╗█████╔╝  ╚████╔╝ ██║     ██║   ██║██╔██╗ ██║██╔██╗ ██║█████╗  ██║        ██║   ");
        System.out.println("          ╚════██║██╔═██╗   ╚██╔╝  ██║     ██║   ██║██║╚██╗██║██║╚██╗██║██╔══╝  ██║        ██║   ");
        System.out.println("          ███████║██║  ██╗   ██║   ╚██████╗╚██████╔╝██║ ╚████║██║ ╚████║███████╗╚██████╗   ██║   ");
        System.out.println("          ╚══════╝╚═╝  ╚═╝   ╚═╝    ╚═════╝ ╚═════╝ ╚═╝  ╚═══╝╚═╝  ╚═══╝╚══════╝ ╚═════╝   ╚═╝   ");
        System.out.println("\n" + RESET);
        
        System.out.print("                                  INITIALIZING SYSTEMS [");
        for(int i=0; i<20; i++) {
            System.out.print("▓");
            try { Thread.sleep(50 + new Random().nextInt(50)); } catch(Exception e){}
        }
        System.out.println("] 100%");
        try { Thread.sleep(500); } catch(Exception e){}
    }

    private static void randomizeSeats() {
        Random r = new Random();
        for (Flight f : DataStore.flights) {
            for (int i = 0; i < 8; i++) {
                if(!f.seats[r.nextInt(8)][r.nextInt(4)]) {
                    f.seats[r.nextInt(8)][r.nextInt(4)] = (r.nextInt(10) > 7); // 20% chance random fill
                }
            }
        }
    }

    private static void authMenu(Scanner sc) {
        clearScreen();
        printBoxed("INDIA SKYCONNECT v7.0");
        
        System.out.println("   [1] LOGIN");
        System.out.println("   [2] REGISTER NEW USER");
        System.out.println("   [3] SHUTDOWN");
        printLine();
        
        int opt = getIntInput(sc, "\n   SELECT > ", 1, 3);

        if (opt == 1) login(sc);
        else if (opt == 2) register(sc);
        else System.exit(0);
    }

    private static void login(Scanner sc) {
        System.out.print("\n   USERNAME: "); String u = sc.nextLine().trim();
        System.out.print("   PASSWORD: "); String p = sc.nextLine().trim();
        loading("   AUTHENTICATING...");

        if (DataStore.users.containsKey(u)) {
            if(DataStore.users.get(u).pass.equals(p)) {
                DataStore.currentUser = DataStore.users.get(u);
                printStatus(true, "ACCESS GRANTED. WELCOME " + DataStore.currentUser.name);
            } else {
                printStatus(false, "ACCESS DENIED: INCORRECT PASSWORD");
            }
        } else {
            printStatus(false, "USER NOT FOUND");
        }
    }

    private static void register(Scanner sc) {
        printHeader("USER REGISTRATION", "DATABASE WRITE");
        System.out.print("   Desired Username: "); String u = sc.nextLine().trim();
        if (DataStore.users.containsKey(u)) {
            printStatus(false, "USERNAME TAKEN"); return;
        }
        System.out.print("   Full Name: "); String n = sc.nextLine().trim();
        System.out.print("   Password: "); String p = sc.nextLine().trim();

        User newUser = new User(u, p, n, false);
        DataStore.users.put(u, newUser);
        DatabaseManager.saveUser(newUser); // Save to CSV
        
        printStatus(true, "ACCOUNT CREATED SUCCESSFULLY");
    }

    private static void userMenu(Scanner sc) {
        clearScreen();
        User u = DataStore.currentUser;
        printHeader("PASSENGER DASHBOARD", u.name.toUpperCase());
        
        System.out.println("   WALLET: " + GREEN + "₹" + formatMoney(u.wallet) + RESET);
        printLine();
        System.out.println(CYAN + "   [1] " + RESET + "BOOK FLIGHT");
        System.out.println(CYAN + "   [2] " + RESET + "SEARCH ROUTES");
        System.out.println(CYAN + "   [3] " + RESET + "MY TRIPS");
        System.out.println(CYAN + "   [4] " + RESET + "CANCEL BOOKING");
        System.out.println(CYAN + "   [5] " + RESET + "ADD FUNDS");
        System.out.println(CYAN + "   [6] " + RESET + "LOGOUT");
        printLine();
        
        int opt = getIntInput(sc, "\n   ENTER CHOICE > ", 1, 6);

        switch (opt) {
            case 1: bookFlight(sc, DataStore.flights); break;
            case 2: searchFlights(sc); break;
            case 3: viewMyTickets(sc, false); break;
            case 4: viewMyTickets(sc, true); break;
            case 5: addFunds(sc); break;
            case 6: logout(); break;
        }
    }

    private static void adminMenu(Scanner sc) {
        clearScreen();
        printHeader("ADMIN CONSOLE", "DB CONNECTED");
        
        System.out.println(CYAN + "   [1] " + RESET + "VIEW MANIFESTS");
        System.out.println(CYAN + "   [2] " + RESET + "ADD FLIGHT (DB)");
        System.out.println(CYAN + "   [3] " + RESET + "REMOVE FLIGHT (DB)");
        System.out.println(CYAN + "   [4] " + RESET + "ANALYTICS");
        System.out.println(CYAN + "   [5] " + RESET + "USER REGISTRY");
        System.out.println(CYAN + "   [6] " + RESET + "LOGOUT");
        printLine();
        
        int opt = getIntInput(sc, "\n   COMMAND > ", 1, 6);
        
        switch(opt) {
            case 1: viewManifests(sc); break;
            case 2: addNewFlight(sc); break;
            case 3: removeFlight(sc); break;
            case 4: viewAnalytics(); break;
            case 5: viewUserRegistry(); break;
            case 6: logout(); break;
        }
    }


    private static void searchFlights(Scanner sc) {
        clearScreen();
        printHeader("SEARCH ENGINE", "QUERY DB");
        System.out.print("   Enter City Code (e.g. DEL) or ENTER for all: ");
        String query = sc.nextLine().toUpperCase().trim();
        
        List<Flight> results = DataStore.flights;
        if (!query.isEmpty()) {
            results = DataStore.flights.stream()
                .filter(f -> f.org.contains(query) || f.dst.contains(query))
                .collect(Collectors.toList());
        }

        if (results.isEmpty()) {
            printStatus(false, "NO FLIGHTS FOUND");
        } else {
            bookFlight(sc, results);
        }
    }

    private static void bookFlight(Scanner sc, List<Flight> availableFlights) {
        clearScreen();
        printHeader("FLIGHT SELECTION", availableFlights.size() + " ROUTES FOUND");

        System.out.printf("   %-4s %-8s %-10s %-10s %-10s %-10s\n", "#", "ID", "ORG", "DST", "TIME", "FARE");
        System.out.println("   " + CYAN + "────────────────────────────────────────────────────────────" + RESET);
        
        for (int i = 0; i < availableFlights.size(); i++) {
            Flight f = availableFlights.get(i);
            String sn = String.format("[%d]", i + 1);
            System.out.printf("   " + WHITE + "%-4s" + RESET + " %-8s %-10s %-10s %-10s ₹%-9.0f\n", sn, f.id, f.org, f.dst, f.time, f.price);
        }

        int selection = getIntInput(sc, "\n   SELECT SERIAL # (0 to back): ", 0, availableFlights.size());
        if (selection == 0) return;

        Flight f = availableFlights.get(selection - 1);

        // Class
        clearScreen();
        printHeader("CABIN CLASS", f.id);
        int i = 1;
        for(CabinClass cc : CabinClass.values()) {
            System.out.printf("   [%d] %-15s (x%.1f Fare)\n", i++, cc.label, cc.multiplier);
        }
        int classIdx = getIntInput(sc, "\n   SELECT CLASS > ", 1, 3) - 1;
        CabinClass selectedClass = CabinClass.values()[classIdx];

        // Seat
        String seatNum = selectSeat(sc, f, selectedClass);
        if (seatNum == null) return;

        // Meal
        clearScreen();
        printHeader("IN-FLIGHT DINING", "CATERING");
        i = 1;
        for(MealType m : MealType.values()) {
            System.out.printf("   [%d] %-15s (+₹%.0f)\n", i++, m.label, m.price);
        }
        int mealIdx = getIntInput(sc, "\n   SELECT MEAL > ", 1, 5) - 1;
        MealType selectedMeal = MealType.values()[mealIdx];

        // Payment
        double baseFare = f.price * selectedClass.multiplier;
        double total = baseFare + selectedMeal.price;
        
        clearScreen();
        printHeader("BILLING INVOICE", "FINAL REVIEW");
        System.out.println("   Flight:    " + f.id + " (" + f.org + " -> " + f.dst + ")");
        System.out.println("   Seat:      " + seatNum + " (" + selectedClass.label + ")");
        System.out.println("   Add-on:    " + selectedMeal.label);
        printLine();
        System.out.println("   TOTAL:     " + GREEN + "₹" + formatMoney(total) + RESET);
        
        if(DataStore.currentUser.wallet < total) {
            printStatus(false, "INSUFFICIENT WALLET BALANCE");
            return;
        }

        System.out.print("\n   CONFIRM PAYMENT? (Y/N): ");
        if(sc.nextLine().equalsIgnoreCase("Y")) {
            loading("   PROCESSING PAYMENT");
            
            DataStore.currentUser.wallet -= total;
            DatabaseManager.updateUserWallet(DataStore.currentUser); // Update DB

            int r = Integer.parseInt(seatNum.substring(0, seatNum.length()-1)) - 1;
            int c = seatNum.charAt(seatNum.length()-1) - 'A';
            f.seats[r][c] = true;

            String pnr = "PNR-" + (1000 + new Random().nextInt(8999));
            Ticket t = new Ticket(f, seatNum, DataStore.currentUser.name, selectedClass, selectedMeal, total, pnr);
            
            DataStore.tickets.add(t);
            DatabaseManager.saveBooking(t); // Save to DB

            printStatus(true, "BOOKING CONFIRMED! PNR: " + pnr);
            
            System.out.print("   View Boarding Pass? (Y/N): ");
            if(sc.nextLine().equalsIgnoreCase("Y")) printBoardingPass(t);
        }
    }

    private static String selectSeat(Scanner sc, Flight f, CabinClass cc) {
        while(true) {
            clearScreen();
            System.out.println(BG_CYAN + BLACK + "    SEAT MAP: " + cc.label + "    " + RESET);
            System.out.println("    Rows " + (cc.startRow + 1) + " - " + cc.endRow);
            System.out.println("\n        A   B     C   D");
            
            for(int r=0; r<8; r++) {
                String color = (r >= cc.startRow && r < cc.endRow) ? WHITE : BLACK;
                System.out.printf(color + "   %2d   ", (r+1));
                
                for(int c=0; c<4; c++) {
                    String sym = f.seats[r][c] ? RED + "[X]" : GREEN + "[ ]";
                    if(r < cc.startRow || r >= cc.endRow) sym = BLACK + "[░]";
                    System.out.print(sym + RESET + " ");
                    if(c==1) System.out.print("    ");
                }
                System.out.println();
            }

            System.out.print("\n   Select Seat (e.g. 1A) or X to Cancel: ");
            String in = sc.nextLine().toUpperCase().trim();
            if(in.equals("X")) return null;

            if(in.length() < 2) continue;
            try {
                int r = Integer.parseInt(in.substring(0, in.length()-1)) - 1;
                int c = in.charAt(in.length()-1) - 'A';

                if (r < cc.startRow || r >= cc.endRow) {
                    System.out.println(RED + "   Restricted Seat Selection." + RESET);
                    pause(); continue;
                }
                if (f.seats[r][c]) {
                    System.out.println(RED + "   Seat Unavailable." + RESET);
                    pause(); continue;
                }
                return in;
            } catch (Exception e) {
                System.out.println(RED + "   Invalid Format." + RESET); pause();
            }
        }
    }

    private static void viewMyTickets(Scanner sc, boolean cancelMode) {
        clearScreen();
        String title = cancelMode ? "CANCELLATION PORTAL" : "MY BOOKINGS";
        printHeader(title, DataStore.currentUser.name);

        List<Ticket> myTix = DataStore.tickets.stream()
            .filter(t -> t.owner.equals(DataStore.currentUser.name))
            .collect(Collectors.toList());

        if (myTix.isEmpty()) {
            printStatus(false, "NO ACTIVE BOOKINGS"); return;
        }

        System.out.printf("   %-4s %-10s %-8s %-15s %-10s\n", "#", "PNR", "ROUTE", "DESTINATION", "STATUS");
        for (int i = 0; i < myTix.size(); i++) {
            Ticket t = myTix.get(i);
            System.out.printf("   [%d]  %-10s %-8s %-15s %s\n", (i+1), t.bookingId, t.f.id, t.f.dst, GREEN + "CONFIRMED" + RESET);
        }

        if (cancelMode) {
            int idx = getIntInput(sc, "\n   Select # to CANCEL (0 to back): ", 0, myTix.size());
            if (idx == 0) return;
            cancelTicket(myTix.get(idx - 1));
        } else {
            int idx = getIntInput(sc, "\n   Select # to View Pass (0 to back): ", 0, myTix.size());
            if (idx == 0) return;
            printBoardingPass(myTix.get(idx - 1));
        }
    }

    private static void cancelTicket(Ticket t) {
        System.out.println(RED + "\n   !!! CONFIRM CANCELLATION !!!" + RESET);
        System.out.println("   Refund Amount: ₹" + formatMoney(t.paidPrice));
        System.out.print("   Proceed? (Y/N): ");
        Scanner sc = new Scanner(System.in);
        if(sc.nextLine().equalsIgnoreCase("Y")) {
            loading("   PROCESSING REFUND");
            int r = Integer.parseInt(t.seat.substring(0, t.seat.length()-1)) - 1;
            int c = t.seat.charAt(t.seat.length()-1) - 'A';
            t.f.seats[r][c] = false;
            
            DataStore.currentUser.wallet += t.paidPrice;
            DatabaseManager.updateUserWallet(DataStore.currentUser); // Update DB

            DataStore.tickets.remove(t);
            DatabaseManager.deleteBooking(t.bookingId); // Update DB

            printStatus(true, "REFUND SUCCESSFUL");
        }
    }

   
    private static void viewManifests(Scanner sc) {
        System.out.print("\n   Enter Flight ID (e.g. AI-101): ");
        String fid = sc.nextLine().toUpperCase();
        
        System.out.println(CYAN + "\n   --- MANIFEST: " + fid + " ---" + RESET);
        boolean found = false;
        for(Ticket t : DataStore.tickets) {
            if(t.f.id.equals(fid)) {
                System.out.printf("   Seat: %-3s | PNR: %-8s | %-15s | %s\n", t.seat, t.bookingId, t.owner, t.travelClass.label);
                found = true;
            }
        }
        if(!found) printStatus(false, "NO PASSENGERS FOUND");
        else pause();
    }

    private static void removeFlight(Scanner sc) {
        System.out.print("\n   Flight ID to DELETE: ");
        String fid = sc.nextLine().toUpperCase();
        
        Flight toRemove = null;
        for(Flight f : DataStore.flights) {
            if(f.id.equals(fid)) { toRemove = f; break; }
        }

        if(toRemove != null) {
            System.out.print(RED + "   ⚠ Confirm deletion? (Y/N): " + RESET);
            if(sc.nextLine().equalsIgnoreCase("Y")) {
                DataStore.flights.remove(toRemove);
                DatabaseManager.deleteFlight(toRemove.id); // DELETE FROM DB
                printStatus(true, "ROUTE DELETED");
            }
        } else { printStatus(false, "FLIGHT NOT FOUND"); }
    }

    private static void viewAnalytics() {
        clearScreen();
        printHeader("SYSTEM ANALYTICS", "LIVE DATA");
        
        double totalRevenue = DataStore.tickets.stream().mapToDouble(t -> t.paidPrice).sum();
        
        System.out.println("   " + GREEN + "Total Revenue:       ₹" + formatMoney(totalRevenue) + RESET);
        System.out.println("   " + CYAN +  "Total Bookings:      " + DataStore.tickets.size() + RESET);
        System.out.println("   " + WHITE + "Registered Users:    " + DataStore.users.size() + RESET);
        System.out.println("   " + WHITE + "Active Routes:       " + DataStore.flights.size() + RESET);
        printLine();
        pause();
    }

    private static void viewUserRegistry() {
        clearScreen();
        printHeader("USER REGISTRY", "ALL USERS");
        System.out.printf("   %-15s | %-20s | %-10s\n", "USERNAME", "FULL NAME", "ROLE");
        System.out.println("   " + CYAN + "────────────────────────────────────────────────────" + RESET);
        
        for(User u : DataStore.users.values()) {
            String role = u.isAdmin ? RED + "ADMIN" + RESET : GREEN + "USER " + RESET;
            System.out.printf("   %-15s | %-20s | %s\n", u.username, u.name, role);
        }
        pause();
    }

    private static void addNewFlight(Scanner sc) {
        System.out.println(CYAN + "\n   --- ADD NEW ROUTE ---" + RESET);
        try {
            System.out.print("   Flight ID: "); String id = sc.nextLine().toUpperCase();
            System.out.print("   Origin: "); String org = sc.nextLine().toUpperCase();
            System.out.print("   Destination: "); String dst = sc.nextLine().toUpperCase();
            System.out.print("   Time (HH:MM): "); String time = sc.nextLine();
            System.out.print("   Price (₹): "); double price = Double.parseDouble(sc.nextLine());
            
            Flight f = new Flight(id, org, dst, time, price);
            DataStore.flights.add(f);
            DatabaseManager.saveFlight(f); // SAVE TO DB
            printStatus(true, "FLIGHT ROUTE ADDED");
        } catch (Exception e) {
            printStatus(false, "INVALID INPUT DATA");
        }
    }

  
    private static void printStatus(boolean success, String msg) {
        String color = success ? BG_GREEN : BG_RED;
        String symbol = success ? "✔" : "✘";
        System.out.println("\n   " + color + WHITE + " " + symbol + " " + msg + " " + RESET);
        pause();
    }

    private static void printBoardingPass(Ticket t) {
        clearScreen();
        String flightDate = LocalDate.now().plusDays(2).format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
        String terminal = "T" + (Math.abs(t.f.id.hashCode()) % 3 + 1);
        String gate = "G" + (Math.abs(t.f.id.hashCode()) % 20 + 1);
        String baggage = t.travelClass == CabinClass.ECONOMY ? "15 KG" : (t.travelClass == CabinClass.BUSINESS ? "30 KG" : "50 KG");
        String priority = t.travelClass != CabinClass.ECONOMY ? GREEN + "PRIORITY" + RESET : "STANDARD";

        String border = CYAN + "╔════════════════════════════════════════════════════════════╗" + RESET;
        String thinBorder = CYAN + "╟────────────────────────────────────────────────────────────╢" + RESET;
        String bottom = CYAN + "╚════════════════════════════════════════════════════════════╝" + RESET;

        System.out.println("\n" + border);
        System.out.println(CYAN + "║ SKYCONNECT " + RESET + WHITE + "| BOARDING PASS" + String.format("%37s", "CLASS: " + t.travelClass.label.toUpperCase()) + CYAN + " ║" + RESET);
        System.out.println(thinBorder);
        System.out.printf(CYAN + "║" + RESET + " %-15s %-15s %-15s            " + CYAN + "║\n" + RESET, "PASSENGER", "FLIGHT", "DATE");
        System.out.printf(CYAN + "║" + RESET + " " + BOLD + "%-15s %-15s %-15s" + RESET + "            " + CYAN + "║\n" + RESET, t.owner.split(" ")[0].toUpperCase(), t.f.id, flightDate);
        System.out.println(CYAN + "║                                                            ║" + RESET);
        System.out.printf(CYAN + "║" + RESET + " %-15s %-15s %-15s            " + CYAN + "║\n" + RESET, "FROM", "TO", "TIME");
        System.out.printf(CYAN + "║" + RESET + " " + BOLD + "%-15s %-15s %-15s" + RESET + "            " + CYAN + "║\n" + RESET, t.f.org, t.f.dst, minus30Mins(t.f.time));
        System.out.println(CYAN + "║                                                            ║" + RESET);
        System.out.printf(CYAN + "║" + RESET + " %-10s %-10s %-15s                   " + CYAN + "║\n" + RESET, "TERM", "GATE", "BAGS");
        System.out.printf(CYAN + "║" + RESET + " " + BOLD + "%-10s %-10s %-15s" + RESET + "                   " + CYAN + "║\n" + RESET, terminal, gate, baggage);
        System.out.println(thinBorder);
        System.out.printf(CYAN + "║" + RESET + " %-10s %-20s %-15s         " + CYAN + "║\n" + RESET, "SEAT", "MEAL", "PNR");
        System.out.printf(CYAN + "║" + RESET + " " + BG_BLUE + WHITE + " %-3s " + RESET + "      " + BOLD + "%-20s %-15s" + RESET + "         " + CYAN + "║\n" + RESET, t.seat, t.meal.label, t.bookingId);
        System.out.println(thinBorder);
        System.out.println(CYAN + "║" + RESET + " GROUP: " + priority + String.format("%48s", " ") + CYAN + "║");
        System.out.println(CYAN + "║" + WHITE + " ||| | ||| || ||| | || |||| | || || | ||| || ||| | ||       " + CYAN + "║" + RESET);
        System.out.println(bottom);
        pause();
    }

    private static void addFunds(Scanner sc) {
        System.out.print("\n   Enter Amount (₹): ");
        try {
            double amt = Double.parseDouble(sc.nextLine());
            DataStore.currentUser.wallet += amt;
            DatabaseManager.updateUserWallet(DataStore.currentUser);
            printStatus(true, "WALLET UPDATED");
        } catch (Exception e) { printStatus(false, "INVALID AMOUNT"); }
    }

    private static int getIntInput(Scanner sc, String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt);
            try {
                String line = sc.nextLine();
                if(line.trim().isEmpty()) continue;
                int input = Integer.parseInt(line.trim());
                if (input >= min && input <= max) return input;
                System.out.println(RED + "   Invalid Range." + RESET);
            } catch (NumberFormatException e) {
                System.out.println(RED + "   Numbers only." + RESET);
            }
        }
    }

    private static String minus30Mins(String time) {
        try {
            String[] parts = time.split(":");
            int h = Integer.parseInt(parts[0]), m = Integer.parseInt(parts[1]) - 30;
            if(m < 0) { m += 60; h -= 1; }
            if (h < 0) h = 23;
            return String.format("%02d:%02d", h, m);
        } catch(Exception e) { return time; }
    }

    private static String formatMoney(double d) { return new DecimalFormat("#,###").format(d); }
    private static void clearScreen() { System.out.print("\033[H\033[2J"); System.out.flush(); }
    private static void loading(String m) { System.out.print(m); try{Thread.sleep(400);}catch(Exception e){} System.out.println(); }
    private static void pause() { System.out.print(BLACK + "\n   [ENTER] to continue..." + RESET); try{System.in.read();}catch(Exception e){} }
    private static void logout() { DataStore.currentUser = null; }
    
    private static void printHeader(String m, String s) { 
        System.out.println(CYAN + "╔════════════════════════════════════════════════════════════╗" + RESET);
        System.out.printf(CYAN + "║" + WHITE + " %-25s %32s " + CYAN + "║\n" + RESET, m, s);
        System.out.println(CYAN + "╚════════════════════════════════════════════════════════════╝" + RESET);
    }
    
    private static void printBoxed(String text) {
        System.out.println(CYAN + "╔════════════════════════════════════════════════════════════╗" + RESET);
        int pad = (58 - text.length()) / 2;
        System.out.printf(CYAN + "║" + WHITE + "%" + pad + "s%s%-" + (58 - pad - text.length()) + "s" + CYAN + "║\n" + RESET, "", text, "");
        System.out.println(CYAN + "╚════════════════════════════════════════════════════════════╝" + RESET);
    }

    private static void printLine() {
        System.out.println(CYAN + "──────────────────────────────────────────────────────────────" + RESET);
    }
    
    // --- CLASSES ---
    static class User {
        String username, pass, name;
        boolean isAdmin;
        double wallet = 75000.00;
        User(String u, String p, String n, boolean a) { username=u; pass=p; name=n; isAdmin=a; }
    }

    static class Flight {
        String id, org, dst, time;
        double price;
        boolean[][] seats = new boolean[8][4];
        Flight(String i, String o, String d, String t, double p) { id=i; org=o; dst=d; time=t; price=p; }
    }

    static class Ticket {
        Flight f;
        String seat, owner, bookingId;
        CabinClass travelClass;
        MealType meal;
        double paidPrice;
        Ticket(Flight f, String s, String o, CabinClass tc, MealType m, double p, String bid) {
            this.f=f; seat=s; owner=o; travelClass=tc; meal=m; paidPrice=p; bookingId=bid;
        }
    }

}
