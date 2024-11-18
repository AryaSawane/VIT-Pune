
class pagefault
{
    public static void main(String args[])
    { System.err.println("FIFO");
        int[] refrence={7,0,1,2,0,3,0,4,2,3,0,3,1,2,0};
        int n=refrence.length;
        int [] frame={-1,-1,-1};
        int hit=0;
        int fault=0;
        int p=0;

        for(int i=0;i<n;i++)
        {boolean flag=false;
            for(int j=0;j<3;j++)
            { 
                if(frame[j]==refrence[i])
                {
                    hit++;
                    flag=true;
                }}

                if(flag==false)
                {
                    frame[p]=refrence[i];
                     p++;
                    fault++;
                    if(p==3)
                    {
                        p=0;
                    }
                  
                        
                    }

                    for(int m=0;m<3;m++)
                    {
                        System.out.print(frame[m]+" ");
                    }
                    System.out.println();
                  
                }
            
        

        System.out.println("page faults are "+fault+ "  and hits are "+hit);




        System.out.println("Optimal ");

        int[] ref={7,0,1,2,0,3,0,4,2,3,0,3,2,1,2,0,1,7,0,1};
        int no=ref.length;
        int [] frm={-1,-1,-1,-1};
        int h=0;
        int flt=0;
        int pntr=0;
        

        for (int i = 0; i < no; i++) {
            boolean flag = false;
            int maxIndex = -1;    
            int notFoundIndex = -1; 
     
            for (int j = 0; j < 4; j++) {
                if (frm[j] == ref[i]) {
                    h++; 
                    flag = true;
                    break;
                }
            }
        
          
            if (!flag) {
            
                if (pntr < 4) {
                    frm[pntr] = ref[i];
                    pntr++;
                    flt++;
                } else { 
                    int farthest = -1;
                    for (int j = 0; j < 4; j++) {
                        boolean found = false;
                        for (int k = i + 1; k < no; k++) {
                            if (frm[j] == ref[k]) {
                                found = true;
                                if (k > farthest) {
                                    farthest = k;
                                    maxIndex = j;
                                }
                                break;
                            }
                        }
                        if (!found && notFoundIndex == -1) {
                            notFoundIndex = j; 
                        }
                    }
        
              
                    if (notFoundIndex != -1) {
                        frm[notFoundIndex] = ref[i];
                    } else {
                        frm[maxIndex] = ref[i];
                    }
                    flt++;
                }
            }
     
            for (int m = 0; m < 4; m++) {
                System.out.print(frm[m] + " ");
            }
            System.out.println();
        }
            
        

        System.out.println("page faults are "+flt+ "  and hits are "+h);


System.out.println("LRU ");
        int[] arr = {7, 0, 1, 2, 0, 3, 0, 4, 2, 3, 0, 3, 2, 1, 2, 0, 1, 7, 0, 1};
        int nom = ref.length;
        int[] frme = {-1, -1, -1, -1}; 
        int[] re = new int[4];
        int pflt = 0; 
        int ph = 0; 

        for (int i = 0; i < nom; i++) {
            boolean pagehit = false; 

            
            for (int j = 0; j < 4; j++) {
                if (frme[j] == arr[i]) {
                    ph++; 
                    pagehit = true;
                    re[j] = i; 
                    break;
                }
            }

            if (!pagehit) { 
                int lruIndex = -1; 
                int min = Integer.MAX_VALUE;

              
                for (int j = 0; j < 4; j++) {
                    if (frme[j] == -1) { 
                        lruIndex = j;
                        break;
                    }
                    if (re[j] < min) {
                        min = re[j];
                        lruIndex = j;
                    }
                }

                frme[lruIndex] = ref[i]; 
                re[lruIndex] = i; 
                pflt++; 
            }

            for (int j = 0; j < 4; j++) {
                System.out.print(frme[j] + " ");
            }
            System.out.println();
        }

    
        System.out.println("Page faults: " + pflt);
        System.out.println("Page hits: " + ph);
    }}

    
