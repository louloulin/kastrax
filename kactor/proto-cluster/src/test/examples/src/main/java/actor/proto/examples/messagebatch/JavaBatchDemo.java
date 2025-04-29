package actor.proto.examples.messagebatch;

import actor.proto.Actor;
import actor.proto.ActorSystem;
import actor.proto.Context;
import actor.proto.PID;
import actor.proto.Props;
import actor.proto.mailbox.MessageBatch;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Simple Java example demonstrating the use of MessageBatch.
 */
public class JavaBatchDemo {
    
    // Define a simple message class
    static class SimpleMessage {
        private final String text;
        
        public SimpleMessage(String text) {
            this.text = text;
        }
        
        public String getText() {
            return text;
        }
    }
    
    // Define a message batch implementation
    static class SimpleMessageBatch implements MessageBatch {
        private final List<Object> messages;
        
        public SimpleMessageBatch(List<Object> messages) {
            this.messages = messages;
        }
        
        @Override
        public List<Object> getMessages() {
            return messages;
        }
    }
    
    public static void main(String[] args) throws Exception {
        System.out.println("Starting MessageBatch demo...");
        
        // Create a latch to wait for all messages to be processed
        final int messageCount = 5;
        final CountDownLatch messagesLatch = new CountDownLatch(messageCount);
        final CountDownLatch batchLatch = new CountDownLatch(1);
        
        // Create an actor system
        ActorSystem system = ActorSystem.create("batch-demo");
        
        // Create an actor that counts down the latch for each message
        Props props = Props.create(() -> new Actor() {
            @Override
            public void receive(Context context) throws Exception {
                Object msg = context.getMessage();
                if (msg instanceof SimpleMessage) {
                    SimpleMessage simpleMsg = (SimpleMessage) msg;
                    System.out.println("Received individual message: " + simpleMsg.getText());
                    messagesLatch.countDown();
                } else if (msg instanceof SimpleMessageBatch) {
                    SimpleMessageBatch batch = (SimpleMessageBatch) msg;
                    System.out.println("Received batch with " + batch.getMessages().size() + " messages");
                    batchLatch.countDown();
                } else {
                    System.out.println("Received unknown message: " + msg);
                }
            }
        });
        
        // Spawn the actor
        PID pid = system.getRoot().spawn(props);
        
        // Create a batch of messages
        List<Object> messages = new ArrayList<>();
        for (int i = 0; i < messageCount; i++) {
            messages.add(new SimpleMessage("Message " + i));
        }
        SimpleMessageBatch batch = new SimpleMessageBatch(messages);
        
        System.out.println("Sending a batch of " + messages.size() + " messages...");
        
        // Send the batch to the actor
        system.getRoot().send(pid, batch);
        
        // Wait for all messages to be processed
        boolean messagesProcessed = messagesLatch.await(5, TimeUnit.SECONDS);
        boolean batchProcessed = batchLatch.await(5, TimeUnit.SECONDS);
        
        // Print results
        System.out.println("\nResults:");
        System.out.println("All individual messages processed: " + messagesProcessed);
        System.out.println("Batch message itself processed: " + batchProcessed);
        
        // Stop the actor
        system.getRoot().stop(pid);
        
        System.out.println("MessageBatch demo completed.");
    }
}
