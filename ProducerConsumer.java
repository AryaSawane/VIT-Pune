import java.util.LinkedList;
import java.util.concurrent.Semaphore;

public class ProducerConsumer {
    public static void main(String[] args) {
        Buffer buffer = new Buffer(5); 

     
        Thread producer = new Thread(new Producer(buffer, 20)); 
        Thread consumer = new Thread(new Consumer(buffer, 20)); 

      
        producer.start();
        consumer.start();
    }
}


class Buffer {
    private LinkedList<Integer> list = new LinkedList<>();
    private int capacity;

 
    private Semaphore mutex = new Semaphore(1); 
    private Semaphore empty; 
    private Semaphore full = new Semaphore(0); 

    public Buffer(int capacity) {
        this.capacity = capacity;
        this.empty = new Semaphore(capacity);
    }

 
    public void produce(int item) throws InterruptedException {
        empty.acquire();  
        mutex.acquire();  

      
        list.add(item);
        System.out.println("Produced: " + item);

        mutex.release(); 
        full.release();  
    }

    public int consume() throws InterruptedException {
        full.acquire();  
        mutex.acquire();  

      
        int item = list.removeFirst();
        System.out.println("Consumed: " + item);

        mutex.release();  
        empty.release();  

        return item;
    }
}


class Producer implements Runnable {
    private Buffer buffer;
    private int itemCount; 

    public Producer(Buffer buffer, int itemCount) {
        this.buffer = buffer;
        this.itemCount = itemCount;
    }

    @Override
    public void run() {
        int item = 0;
        try {
            while (item < itemCount) {
                buffer.produce(item++); 
                Thread.sleep(500); 
            }
        } catch (InterruptedException e) {
            System.out.println("Producer interrupted: " + e.getMessage());
        }
    }
}


class Consumer implements Runnable {
    private Buffer buffer;
    private int itemCount;

    public Consumer(Buffer buffer, int itemCount) {
        this.buffer = buffer;
        this.itemCount = itemCount;
    }

    @Override
    public void run() {
        int itemsConsumed = 0;
        try {
            while (itemsConsumed < itemCount) {
                buffer.consume();
                itemsConsumed++;
                Thread.sleep(1000);
            }
        } catch (InterruptedException e) {
            System.out.println("Consumer interrupted: " + e.getMessage());
        }
    }
}
