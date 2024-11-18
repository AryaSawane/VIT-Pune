class mainpriorityP {
    public static void main(String[] args) {
        int[] arrivalTime = {0, 1, 2, 4}; 
        int[] burstTime = {5, 4,2,1};
        int[] priority={2,1,3,4};  
        int n = arrivalTime.length;

        int[] completionTime = new int[n];
        int[] turnAroundTime = new int[n];
        int[] waitingTime = new int[n];
        int[] remainingTime = burstTime.clone();

        int completed = 0;     
        int currentTime = 0;   
        int shortest = -1;     
        boolean foundProcess;  

        while (completed < n) {
            foundProcess = false;
            int minBurstTime = Integer.MAX_VALUE;
            int pri=-1;

            
            for (int i = 0; i < n; i++) {
                if (arrivalTime[i] <= currentTime && remainingTime[i] > 0 && priority[i]>pri) {
                    shortest = i;
                    pri=priority[i];
                    foundProcess = true;
                }
            }

            if (!foundProcess) {
                currentTime++;
                continue;
            }

    
            remainingTime[shortest]--;
            currentTime++;

            
            if (remainingTime[shortest] == 0) {
                completed++;
                completionTime[shortest] = currentTime;

                
                turnAroundTime[shortest] = completionTime[shortest] - arrivalTime[shortest];
                waitingTime[shortest] = turnAroundTime[shortest] - burstTime[shortest];
            }
        }

    
        System.out.println("P\tAT\tBT\tCT\tTAT\tWT");
        for (int i = 0; i < n; i++) {
            System.out.println("P" + (i + 1) + "\t" + arrivalTime[i] + "\t" + burstTime[i] + "\t" +
                    completionTime[i] + "\t" + turnAroundTime[i] + "\t" + waitingTime[i]);
        }
    }
}
