
class multithreadmatrix extends Thread {
    private int row; 
    private int[][] matA, matB, result;


    public multithreadmatrix(int row, int[][] matA, int[][] matB, int[][] result) {
        this.row = row;
        this.matA = matA;
        this.matB = matB;
        this.result = result;
    }

    @Override
    public void run() {
        int colsB = matB[0].length; 
        int colsA = matA[0].length;

        for (int j = 0; j < colsB; j++) {
            for (int k = 0; k < colsA; k++) {
                result[row][j] += matA[row][k] * matB[k][j];
            }
        }
    }
}

public class MatrixMultiplication {
    public static void main(String[] args) {
       
        int[][] matA = {
            {1, 2, 3},
            {4, 5, 6},
            {7, 8, 9}
        };
        int[][] matB = {
            {9, 8, 7},
            {6, 5, 4},
            {3, 2, 1}
        };

        int rowsA = matA.length;
        int colsA = matA[0].length;
        int rowsB = matB.length;
        int colsB = matB[0].length;

        
        if (colsA != rowsB) {
            System.out.println("not possible");
            return;
        }

      
        int[][] result = new int[rowsA][colsB];

  
        Thread[] threads = new Thread[rowsA];
        for (int i = 0; i < rowsA; i++) {
            threads[i] = new multithreadmatrix(i, matA, matB, result);
            threads[i].start();
        }

       for (int i = 0; i < rowsA; i++) {
           try {
              threads[i].join();
            } catch (InterruptedException e) {
                System.out.println("Thread interrupted: " + e.getMessage());
           }
        }

     
        System.out.println("Resultant Matrix:");
        for (int i = 0; i < rowsA; i++) {
            for (int j = 0; j < colsB; j++) {
                System.out.print(result[i][j] + " ");
            }
            System.out.println();
        }
    }
}
