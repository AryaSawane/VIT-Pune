import java.util.Arrays;

public class ProcessControlSimulation {
    public static void main(String[] args) throws InterruptedException {
        int[] numbers = {12, 7, 3, 19, 21, 5};

        System.out.println("Main Process (Parent): Original Array: " + Arrays.toString(numbers));

       
        Thread child = new Thread(() -> {
            try {
                System.out.println("Child Process: Sorting Array...");
                bubbleSort(numbers.clone()); 
                System.out.println("Child Process: Sorted Array: " + Arrays.toString(numbers));
            } catch (InterruptedException e) {
                System.out.println("Child Process interrupted.");
            }
        });

   
        child.start();

      
        System.out.println("Parent Process: Sorting Array...");
        selectionSort(numbers); 
        System.out.println("Parent Process: Sorted Array: " + Arrays.toString(numbers));

   
        child.join();
        System.out.println("Parent Process: Child process has completed.");

       
        demonstrateZombieAndOrphanStates();
    }


    private static void bubbleSort(int[] arr) throws InterruptedException {
        for (int i = 0; i < arr.length - 1; i++) {
            for (int j = 0; j < arr.length - i - 1; j++) {
                if (arr[j] > arr[j + 1]) {
                    int temp = arr[j];
                    arr[j] = arr[j + 1];
                    arr[j + 1] = temp;
                }
            }
            Thread.sleep(500); 
        }
    }

    
    private static void selectionSort(int[] arr) {
        for (int i = 0; i < arr.length - 1; i++) {
            int minIndex = i;
            for (int j = i + 1; j < arr.length; j++) {
                if (arr[j] < arr[minIndex]) {
                    minIndex = j;
                }
            }
            int temp = arr[minIndex];
            arr[minIndex] = arr[i];
            arr[i] = temp;
        }
    }

   
    private static void demonstrateZombieAndOrphanStates() throws InterruptedException {
        System.out.println("\nDemonstrating Zombie and Orphan States...");

       
        Thread zombieChild = new Thread(() -> {
            try {
                System.out.println("Zombie Child Process: Running...");
                Thread.sleep(1000);
                System.out.println("Zombie Child Process: Completed.");
            } catch (InterruptedException e) {
                System.out.println("Zombie Child Process interrupted.");
            }
        });
        zombieChild.start();
        Thread.sleep(500); 
        System.out.println("Parent Process: Exiting before the child.");

    
        Thread orphanChild = new Thread(() -> {
            try {
                System.out.println("Orphan Child Process: Running...");
                Thread.sleep(1000);
                System.out.println("Orphan Child Process: Completed.");
            } catch (InterruptedException e) {
                System.out.println("Orphan Child Process interrupted.");
            }
        });
        orphanChild.start();
        orphanChild.join();
    }
}
