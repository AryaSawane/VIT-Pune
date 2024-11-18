import java.util.ArrayList;
import java.util.Scanner;

class AddressBook {
    static class Contact {
        String name;
        String phoneNumber;
        String email;

        Contact(String name, String phoneNumber, String email) {
            this.name = name;
            this.phoneNumber = phoneNumber;
            this.email = email;
        }

        @Override
        public String toString() {
            return "Name: " + name + ", Phone: " + phoneNumber + ", Email: " + email;
        }
    }

    private final ArrayList<Contact> contacts = new ArrayList<>();

    
    public void createAddressBook() {
        contacts.clear();
        System.out.println("Addressbook created ");
    }


    public void viewAddressBook() {
        if (contacts.isEmpty()) {
            System.out.println("empty.");
        } else {
            System.out.println("AddressBook:");
            for (int i = 0; i < contacts.size(); i++) {
                System.out.println((i + 1) + ". " + contacts.get(i));
            }
        }
    }

   
    public void insertRecord(String name, String phoneNumber, String email) {
        contacts.add(new Contact(name, phoneNumber, email));
        System.out.println("inserted ");
    }

    
    public void deleteRecord(int index) {
        if (index < 0 || index >= contacts.size()) {
            System.out.println("Invalid ");
        } else {
            contacts.remove(index);
            System.out.println(" deleted ");
        }
    }


    public void modifyRecord(int index, String name, String phoneNumber, String email) {
        if (index < 0 || index >= contacts.size()) {
            System.out.println("Invalid ");
        } else {
            contacts.set(index, new Contact(name, phoneNumber, email));
            System.out.println("modified ");
        }
    }

    public static void main(String[] args) {
        AddressBook addressBook = new AddressBook();
        Scanner scanner = new Scanner(System.in);
        int choice;

        do {
           
            System.out.println("1. Create Address Book");
            System.out.println("2. View Address Book");
            System.out.println("3. Insert a Record");
            System.out.println("4. Delete a Record");
            System.out.println("5. Modify a Record");
            System.out.println("6. Exit");
            System.out.print("Enter your choice: ");
            choice = scanner.nextInt();
            scanner.nextLine(); 

            switch (choice) {
                case 1: 
                    addressBook.createAddressBook();
                    break;

                case 2: 
                    addressBook.viewAddressBook();
                    break;

                case 3: 
                    System.out.print("Enter Name: ");
                    String name = scanner.nextLine();
                    System.out.print("Enter Phone Number: ");
                    String phoneNumber = scanner.nextLine();
                    System.out.print("Enter Email: ");
                    String email = scanner.nextLine();
                    addressBook.insertRecord(name, phoneNumber, email);
                    break;

                case 4: 
                    System.out.print("Enter the record number  ");
                    int deleteIndex = scanner.nextInt() - 1;
                    addressBook.deleteRecord(deleteIndex);
                    break;

                case 5: 
                    System.out.print("Enter the record number  ");
                    int modifyIndex = scanner.nextInt() - 1;
                    scanner.nextLine(); 
                    System.out.print("Enter New Name: ");
                    name = scanner.nextLine();
                    System.out.print("Enter New Phone Number: ");
                    phoneNumber = scanner.nextLine();
                    System.out.print("Enter New Email: ");
                    email = scanner.nextLine();
                    addressBook.modifyRecord(modifyIndex, name, phoneNumber, email);
                    break;

                case 6:
                    System.out.println("Exiting");
                    break;

                default:
                    System.out.println("Invalid choice.");
            }
        } while (choice != 6);

        scanner.close();
    }
}
