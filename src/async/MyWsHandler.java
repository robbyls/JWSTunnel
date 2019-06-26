

package async;

import javax.websocket.Endpoint;
import javax.websocket.EndpointConfig;
import javax.websocket.MessageHandler;
import javax.websocket.Session;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousSocketChannel;
import java.nio.channels.CompletionHandler;
import java.util.HashMap;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;

public class MyWsHandler extends Endpoint
{

	 boolean init = false;
	 long i =0;
	 int counter =0;
	 ByteArrayOutputStream baos = new ByteArrayOutputStream();
	 ReentrantLock lock = new ReentrantLock();
	 HashMap<String,AsynchronousSocketChannel> map = new HashMap<String,AsynchronousSocketChannel>();
	 

	
	class Attach {
		public AsynchronousSocketChannel client;
		public Session channel;
	}
		
   void readFromServer(Session channel,AsynchronousSocketChannel client){	
	final ByteBuffer buffer = ByteBuffer.allocate(50000);
	Attach attach = new Attach();
	attach.client = client;
	attach.channel = channel;

							client.read(buffer, attach, new CompletionHandler<Integer, Attach>() {
								@Override
								public void completed(Integer result, final Attach scAttachment) {	
									counter++;
									buffer.clear();
										try {					
										
										  if(buffer.hasRemaining() && result>=0)
										  {												
												byte arr[] = new byte[result];	
												ByteBuffer b = buffer.get(arr,0,result);												
												baos.write(arr,0,result);												
												ByteBuffer q = ByteBuffer.wrap(baos.toByteArray());
												scAttachment.channel.getBasicRemote().sendBinary(q);
												System.out.println("Sent " + baos.size());
												String message = new String(buffer.array()).trim();		
												baos = new ByteArrayOutputStream();
												readFromServer(scAttachment.channel,scAttachment.client);
											}else{
												if(result > 0)
												{
													byte arr[] = new byte[result];
													ByteBuffer b = buffer.get(arr,0,result);
													baos.write(arr,0,result);				
													readFromServer(scAttachment.channel,scAttachment.client);
												}
											}
											} catch (Exception e) {
												e.printStackTrace();
											}																										

								}
								@Override
								public void failed(Throwable t, Attach scAttachment) {
									t.printStackTrace();
								}								
							});	
	
	}

	
	void writeToServer(ByteBuffer z,Session channel,AsynchronousSocketChannel client){
		System.out.println("Received " + z.capacity());
		client.write(z, z, new CompletionHandler<Integer, ByteBuffer>() {		
										@Override
										public void completed(Integer result, ByteBuffer attach) {
											z.flip();											
											try{
											//System.out.println("HASH : " + DatatypeConverter.printHexBinary(MessageDigest.getInstance("MD5").digest(attach.array())));
											}catch(Exception e){}
										}
										
										@Override
										public void failed(Throwable t, ByteBuffer asy) {
											t.printStackTrace();
										}	
		});
	}
	
	void process(ByteBuffer z,Session channel)
	{
		lock.lock();
		try{
			if(i>1)
			{
				AsynchronousSocketChannel client = (AsynchronousSocketChannel) map.get(channel.getId());
				writeToServer(z,channel,client);										
			}
			else if(i==1)
			{
				String values = new String(z.array());
				String[] array = values.split("\\|"); 				
				System.out.println("Connected to " + array[0] + " on port " + array[1]);
				AsynchronousSocketChannel client = AsynchronousSocketChannel.open();
				int po = Integer.parseInt(array[1]);
				InetSocketAddress hostAddress = new InetSocketAddress(array[0], po);
				Future future = client.connect(hostAddress);
				future.get(); // returns null
				System.out.println("Client is started: " + client.isOpen());
				map.put(channel.getId(), client);
				init=false;
				readFromServer(channel,client);
			}
			}catch(Exception e){
				e.printStackTrace();
			  }finally{
				lock.unlock();
			  }
	}
		
	 @Override
    public void onOpen(final Session session, EndpointConfig config) {
		i=0;
		System.out.println("New Session " + session.getId());
        session.addMessageHandler(new MessageHandler.Whole<ByteBuffer>() {			
            @Override
            public void onMessage(ByteBuffer message) {
                try {
					message.clear();
					i++;
					System.out.println("Packet # " + i + " for Session " + session.getId() );
					process(message,session);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }
	
}
