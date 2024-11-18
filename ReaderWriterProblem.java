class ReaderWriterProblem{
    public static void main(String[] args) {
        SharedResource resource = new SharedResource(5);

        
        Thread writer1 = new Thread(new Writer(resource, "Writer-1"));
        Thread writer2 = new Thread(new Writer(resource, "Writer-2"));

        Thread reader1 = new Thread(new Reader(resource, "Reader-1"));
        Thread reader2 = new Thread(new Reader(resource, "Reader-2"));
        Thread reader3 = new Thread(new Reader(resource, "Reader-3"));

       
        writer1.start();
        writer2.start();
        reader1.start();
        reader2.start();
        reader3.start();
    }
}


class SharedResource {
     int data = 0; 
     int readerCount = 0; 
    int writeCount = 0;
     int readCount = 0;
    final int limit; 

    private final Object readLock = new Object();
    private final Object writeLock = new Object();

    public SharedResource(int limit) {
        this.limit = limit;
    }

 
    public void startReading(String readerName) throws InterruptedException {
        synchronized (readLock) {
        
            if (readCount >= limit) {
                return;
            }

            readerCount++;
            if (readerCount == 1) {
                synchronized (writeLock) {
                    
                }
            }
        }
        System.out.println(readerName + " is reading. Data = " + data);
    }

   
    public void stopReading(String readerName) {
        synchronized (readLock) {
            readerCount--;
            if (readerCount == 0) {
                synchronized (writeLock) {
                  
                }
            }
          
            readCount++;
        }
        System.out.println(readerName + " finished reading."+ data);
    }

    public void startWriting(String writerName) throws InterruptedException {
        synchronized (writeLock) {
           
            if (writeCount >= limit) {
                return;
            }

            System.out.println(writerName + " is writing.");
            data++;
            writeCount++;
            Thread.sleep(1000);
            System.out.println(writerName + " finished writing. New Data = " + data);
        }
    }
}


class Reader implements Runnable {
    private final SharedResource resource;
    private final String name;

    public Reader(SharedResource resource, String name) {
        this.resource = resource;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            while (true) {
                synchronized (resource) {
                    if (resource.readCount >= resource.limit) {
                        break; 
                    }
                }

                resource.startReading(name);
                Thread.sleep(500); 
                resource.stopReading(name);
                Thread.sleep(1000); 
            }
        } catch (InterruptedException e) {
            System.out.println(name + " interrupted.");
        }
    }
}


class Writer implements Runnable {
    private final SharedResource resource;
    private final String name;

    public Writer(SharedResource resource, String name) {
        this.resource = resource;
        this.name = name;
    }

    @Override
    public void run() {
        try {
            while (true) {
                synchronized (resource) {
                    if (resource.writeCount >= resource.limit) {
                        break; 
                    }
                }

                resource.startWriting(name);
                Thread.sleep(2000); 
            }
        } catch (InterruptedException e) {
            System.out.println(name + " interrupted.");
        }
    }
}
