import java.util.*;

public class DiskScheduling {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);

        // Input number of requests and their positions
        System.out.print("Enter the number of disk requests: ");
        int n = sc.nextInt();
        int[] requests = new int[n];
        System.out.println("Enter the disk requests:");
        for (int i = 0; i < n; i++) {
            requests[i] = sc.nextInt();
        }

        // Input initial head position
        System.out.print("Enter the initial head position: ");
        int head = sc.nextInt();

        // Input disk size and direction
        System.out.print("Enter the disk size (total cylinders): ");
        int diskSize = sc.nextInt();
        System.out.print("Enter the direction (1 for upward, 0 for downward): ");
        boolean direction = sc.nextInt() == 1;

        System.out.println("\nSSTF:");
        sstf(requests, head);

        System.out.println("\nSCAN:");
        scan(requests, head, diskSize, direction);

        System.out.println("\nC-LOOK:");
        cLook(requests, head, direction);
    }

    // SSTF (Shortest Seek Time First) Algorithm
    public static void sstf(int[] requests, int head) {
        int n = requests.length;
        boolean[] visited = new boolean[n];
        int totalMovement = 0;
        int currentHead = head;

        for (int i = 0; i < n; i++) {
            int closest = -1;
            int closestDistance = Integer.MAX_VALUE;

            for (int j = 0; j < n; j++) {
                if (!visited[j] && Math.abs(requests[j] - currentHead) < closestDistance) {
                    closest = j;
                    closestDistance = Math.abs(requests[j] - currentHead);
                }
            }

            visited[closest] = true;
            totalMovement += closestDistance;
            currentHead = requests[closest];
            System.out.println("Head moved to: " + currentHead);
        }

        System.out.println("Total Head Movement: " + totalMovement);
    }

    // SCAN Algorithm
    public static void scan(int[] requests, int head, int diskSize, boolean direction) {
        Arrays.sort(requests);
        int totalMovement = 0;
        int currentHead = head;

        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();

        for (int request : requests) {
            if (request < head) {
                left.add(request);
            } else {
                right.add(request);
            }
        }

        if (!direction) {
            Collections.reverse(left);
        }

        List<Integer> order = direction ? merge(right, left) : merge(left, right);

        for (int request : order) {
            totalMovement += Math.abs(request - currentHead);
            currentHead = request;
            System.out.println("Head moved to: " + currentHead);
        }

        System.out.println("Total Head Movement: " + totalMovement);
    }

    // C-LOOK Algorithm
    public static void cLook(int[] requests, int head, boolean direction) {
        Arrays.sort(requests);
        int totalMovement = 0;
        int currentHead = head;

        List<Integer> left = new ArrayList<>();
        List<Integer> right = new ArrayList<>();

        for (int request : requests) {
            if (request < head) {
                left.add(request);
            } else {
                right.add(request);
            }
        }

        List<Integer> order = direction ? merge(right, left) : merge(left, right);

        for (int request : order) {
            totalMovement += Math.abs(request - currentHead);
            currentHead = request;
            System.out.println("Head moved to: " + currentHead);
        }

        System.out.println("Total Head Movement: " + totalMovement);
    }

    // Helper function to merge lists
    private static List<Integer> merge(List<Integer> first, List<Integer> second) {
        List<Integer> result = new ArrayList<>(first);
        result.addAll(second);
        return result;
    }
}
