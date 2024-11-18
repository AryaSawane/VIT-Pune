public class mainfcfs {

    public static void main(String args[]) {
        int[] p = {0, 1, 5, 6}; 
        int[] b = {2, 2, 3, 4}; 

        
        int[] c = new int[p.length];
        int[] tat = new int[p.length];
        int[] wt = new int[p.length];
        int [] check={0,0,0,0};

      
        int comp=0;

int n=p.length;
int curntime=0;

        while(comp<n)

        {
            int sh=0;
            int at=9999;

            for(int i=0;i<n;i++)
            {
                if(check[i]==0 && p[i]<at)
               { at=p[i];
                sh=i;}
            }

            if(at>curntime)
            {
                curntime++;
            }
            else
            {
                curntime=curntime+b[sh];
                c[sh]=curntime;
                tat[sh]=c[sh]-p[sh];
                wt[sh]=tat[sh]-b[sh];
                check[sh]=1;
                comp++;


            }



        
        }
           int i=0;
        while(i<n)
        {
            System.out.println("P" + (i+1) + "\t" + p[i] + "\t" + b[i] + "\t" + c[i] + "\t\t" + tat[i] + "\t\t" + wt[i]);
            i++;
        }
    }

   
}
