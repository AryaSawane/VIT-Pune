class RoundRobin {
    public static void main(String[] args) {
  
        int n = 4;
        int[] processId = {1, 2, 3, 4};
        int[] burstTime = {8, 5, 10, 3};
        int timeQuantum = 2;
        int[] remainingTime = burstTime.clone();
        int[] waitingTime = new int[n];
        int[] turnaroundTime = new int[n];

        int currentTime = 0;

      
        boolean allDone;
        do {
            allDone = true;
            for (int i = 0; i < n; i++) {
                if (remainingTime[i] > 0) {
                    allDone = false; 
                    if (remainingTime[i] > timeQuantum) {
                        currentTime += timeQuantum;
                        remainingTime[i] -= timeQuantum;
                        
                    } else {
                        currentTime += remainingTime[i];
                        
                        remainingTime[i] = 0;

                        turnaroundTime[i] = currentTime;

                      
                        waitingTime[i] = turnaroundTime[i] - burstTime[i];
                    }
                }
            }
        } while (!allDone);

    
        System.out.println("\nProcess\tBurst Time\tTurnaround Time\tWaiting Time");
        for (int i = 0; i < n; i++) {
            System.out.println(
                    "P" + processId[i] + "\t\t" + burstTime[i] + "\t\t" + turnaroundTime[i] + "\t\t" + waitingTime[i]);
        }
    }
}
