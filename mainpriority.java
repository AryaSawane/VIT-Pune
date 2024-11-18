
class mainpriority
{
    public static void main(String args[])
    {
        int[] p={1,4,3,0};
        int[] b={3,1,3,4};
        int[] c = new int[p.length];
        int[] tat = new int[p.length];
        int[] wt = new int[p.length];
        int[] priority={2,1,3,4};
        int[] check={0,0,0,0,0};
int comp=0;
int n=p.length;
int curntime=0;

        while(comp<n)
        {int minburst=9999999;
         int shortest=-1;
         int pri=-1;
            int ar=9999;
            for(int i=0;i<n;i++)
            {
               if(check[i]==0 && p[i]<=curntime && priority[i]>pri && p[i]<ar)
               {
                shortest=i;
                pri=priority[i];
               }
            }

            if(shortest==-1)
            {
                curntime++;
            }
            else
            {
                curntime+=b[shortest];
                c[shortest]=curntime;
                tat[shortest]=c[shortest]-p[shortest];
                wt[shortest]=tat[shortest]-b[shortest];
                check[shortest]=1;
                comp++;
            }

            
        }

        for(int i=0;i<n;i++)
        {
            System.out.println("P" + (i+1) + "\t" + p[i] + "\t" + b[i] + "\t" + c[i] + "\t\t" + tat[i] + "\t\t" + wt[i]);
        }
        
      
        
    }

  
    
}