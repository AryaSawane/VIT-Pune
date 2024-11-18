
class bannker
{
    public static void main(String args[])
    {
        int p=5;
        int[][] allocate={
            {0,1,0},
            {2,0,0},
            {3,0,2},
            {2,1,1},
            {0,0,2},
        };

        int[][] max={
            {7,5,3},
            {3,2,2},
            {6,0,2},
            {4,2,2},
            {5,3,3}
        };
        int[][] need=new int[p][3];

        int A=3;
        int B=3;
        int C=2;

        for(int i=0;i<p;i++)
        {
            for( int j=0;j<3;j++)
            {
                need[i][j]=max[i][j]-allocate[i][j];
                System.out.print(need[i][j]+"  ");
            }
            System.out.println();
        }

        int comp=0;
        int[] check={0,0,0,0,0};
        int[] seq=new int[p]; 
        int m=0;
        int[] actualseq={1,2,3,4,5};

        while(comp<p)
        { 
          boolean f=false;

           for(int i=0;i<p;i++)
           {
            if(check[i]!=1 && need[i][0]<=A  && need[i][1]<=B && need[i][2]<=C)
           { check[i]=1;
            comp++;
            seq[m]=actualseq[i];
            m++;
            A=A+allocate[i][0];
            B=B+allocate[i][1];
            C=C+allocate[i][2];
        f=true;}



           }

           if(f==false)
           {
            System.err.println("not safe");
            return;
           }
        }
        System.err.println("safe");
        for(int i=0;i<p;i++)
        {
       System.out.print("P"+seq[i]+"->");
   
   }

       



    }
}