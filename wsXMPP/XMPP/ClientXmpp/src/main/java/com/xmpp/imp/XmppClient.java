package com.xmpp.imp;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManager;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.MessageListener;
import org.jivesoftware.smack.PacketCollector;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.Roster;
import org.jivesoftware.smack.RosterEntry;
import org.jivesoftware.smack.RosterGroup;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.XMPPException;
import org.jivesoftware.smack.filter.AndFilter;
import org.jivesoftware.smack.filter.FromContainsFilter;
import org.jivesoftware.smack.filter.PacketFilter;
import org.jivesoftware.smack.filter.PacketTypeFilter;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.packet.Presence;
import org.jivesoftware.smack.packet.Presence.Type;
import org.jivesoftware.smackx.ChatState;
import org.jivesoftware.smackx.ChatStateListener;
import org.jivesoftware.smackx.Form;
import org.jivesoftware.smackx.FormField;
import org.jivesoftware.smackx.filetransfer.FileTransfer.Status;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.muc.MultiUserChat;

public class XmppClient {

	// Global variables
	private String server;
	private int port;

	private ConnectionConfiguration config;
	private XMPPConnection connection;

	private ChatManager chatManager;
	private MessageListener messageListener;

	/**
	 * Sets the server and port global variables
	 * 
	 * @param server
	 * @param port
	 */
	public XmppClient(String server, int port) {
		this.server = server;
		this.port = port;
	}

	/**
	 * Initialize the connection with the server and sets the listener for the chat
	 * 
	 * @throws XMPPException
	 */

	public void init() throws XMPPException {
		System.out.println(String.format("Iniciando conexion al server %1$s port %2$d", server, port));

		// Sets configuration of the server
		config = new ConnectionConfiguration(server, port);

		// Generate the connection with the server
		connection = new XMPPConnection(config);
		connection.connect();

		System.out.println("conectado: " + connection.isConnected());

		/*
		 * Start chatManager to create later a new chat, start messageListener to keep
		 * listening while the conversation takes place
		 */
		chatManager = connection.getChatManager();
		messageListener = new MyMessageListener();
		// GroupChat group = new GroupChat(connection, "RoomPrueba");
		// group.join("amanda1");
		// System.out.println(group.getParticipantCount());

		/**
		 * Create chat listener to be updated of any new message in the conversation
		 */
		connection.getChatManager().addChatListener(new ChatManagerListener() {

			/**
			 * When chat is created adds the listener to it
			 */
			public void chatCreated(final Chat arg0, final boolean arg1) {
				// TODO Auto-generated method stub

				arg0.addMessageListener(new MessageListener() {

					// Process the messages that come to the user
					public void processMessage(Chat arg0, Message arg1) {
						// TODO Auto-generated method stub

						System.out.println("is typing......");
						String from = arg1.getFrom();
						String body = arg1.getBody();
						System.out.println(String.format("Recibiendo mensaje '%1$s' from %2$s", body, from));
					}
				});
			}
		});

	}

	/**
	 * Performs the login with the user and password given
	 * 
	 * @param username
	 * @param password
	 * @throws XMPPException
	 */
	public void performLogin(String username, String password) throws XMPPException {
		// Validate that the connection with the server is established and is not null
		// and then performs the login
		if (connection != null && connection.isConnected()) {
			connection.login(username, password);
			System.out.println("Conectado");
		}
	}

	/**
	 * Sets the user status as available
	 * 
	 * @param available
	 * @param status
	 */
	public void setStatus(boolean available, String status) {
		// Defines the type as available
		Presence.Type type = available ? Type.available : Type.unavailable;
		Presence presence = new Presence(type);
		// Set the status
		presence.setStatus(status);
		connection.sendPacket(presence);

	}

	/**
	 * Disconnect the session
	 */
	public void destroy() {
		if (connection != null && connection.isConnected()) {
			connection.disconnect();
		}
	}

	/**
	 * Sends the message and creates a new chat
	 * 
	 * @param message
	 * @param buddyJID
	 * @throws XMPPException
	 */
	public void sendMessage(String message, String buddyJID) throws XMPPException {
		System.out.println(String.format("Enviando mensaje '%1$s' to user %2$s", message, buddyJID));
		// Send the message to the buddy and sets the message listener for the chat
		Chat chat = chatManager.createChat(buddyJID, messageListener);
		chat.sendMessage(message);
	}

	/**
	 * Creates an entry with a buddy for a chat
	 * 
	 * @param user
	 * @param name
	 * @throws Exception
	 */
	public void createEntry(String user, String name) throws Exception {
		System.out.println(String.format("Creando entrada para compa�ero '%1$s' with name %2$s", user, name));
		// Generates the roster and creates the entry with the buddy
		Roster roster = connection.getRoster();
		roster.createEntry(user, name, null);
	}

	/**
	 * Class with a message and chat listener to create a better performance
	 * 
	 * @author User
	 *
	 */
	class MyMessageListener implements MessageListener, ChatStateListener {

		// When status of the chat change it show the new state
		public void stateChanged(Chat chat, ChatState chatState) {
			if (ChatState.composing.equals(chatState)) {
				System.out.println("Chat State" + chat.getParticipant() + " is typing..");
			} else if (ChatState.gone.equals(chatState)) {
				System.out.println("Chat State" + chat.getParticipant() + " has left the conversation.");
			} else {
				System.out.println("Chat State" + chat.getParticipant() + ": " + chatState.name());
			}
		}

		// Get the messages send in the chat
		public void processMessage(Chat chat, Message message) {
			String from = message.getFrom();
			String body = message.getBody();
			String.format("Recibiendo mensaje '%1$s' from %2$s", body, from);

		}

	}

	/**
	 * Verifies the connection is still connected
	 * 
	 * @return
	 */
	public boolean isConnected() {
		return connection.isConnected();
	}

	/**
	 * Create a new account user, also verifies when an account is already created
	 * 
	 * @param user
	 * @param password
	 */
	public void createAccountForUser(String user, String password) {
		// Set the connection configuration
		ConnectionConfiguration config = new ConnectionConfiguration("alumchat.xyz", 5222);

		// Create the connection
		XMPPConnection connection = new XMPPConnection(config);
		try {
			connection.connect();
		} catch (XMPPException e) {

			e.printStackTrace();
		}

		// Start an accountManager to manage the account creation
		AccountManager accountManager = connection.getAccountManager();
		try {
			// Invoke method to create the account
			accountManager.createAccount(user, password);
		} catch (XMPPException e) {

			System.out.println("error: " + e.getMessage());
		}

		System.out.println("cuenta creada");

	}

	/**
	 * Delete the current session account
	 */
	public void deleteAccountForUser() {
		// Set the connection configuration
		ConnectionConfiguration config = new ConnectionConfiguration("alumchat.xyz", 5222);

		// Create the connection
		XMPPConnection connection = new XMPPConnection(config);
		try {
			connection.connect();
		} catch (XMPPException e) {

			e.printStackTrace();
		}

		// Start an accountManager to manage the current session account delete
		AccountManager accountManager = connection.getAccountManager();
		try {
			accountManager.deleteAccount();
		} catch (XMPPException e) {

			System.out.println("error: " + e.getMessage());
		}

		System.out.println("cuenta eliminada");

	}

	/**
	 * Reads all users and its state
	 */
	public void userConnected() {
		// Generate the roster with the current connection
		Roster roster = connection.getRoster();
		// Gets all the users in the roster
		Collection<RosterEntry> entries = roster.getEntries();
		Presence presence = null;

		// Iterate the entries (users)
		for (RosterEntry entry : entries) {
			// Looks for an specific entry
			presence = roster.getPresence(entry.getUser() + "@alumchat.xyz");

			// Validates if the entry is available or not
			if (presence.getType() == Presence.Type.available) {
				System.out.println("DISPONIBLE " + entry.getUser() + " - " + presence.getType());
			} else {
				System.out.println("DESACTIVADO " + entry.getUser() + " - " + presence.getType());
			}

		}

	}

	/**
	 * Shows the current user information
	 * 
	 * @param user
	 */
	public void userInfo(String user) {
		// Generate the roster with the current connection
		Roster roster = connection.getRoster();
		// Gets all the users in the roster
		Collection<RosterEntry> entries = roster.getEntries();
		Presence presence = null;
		int flag = 0;

		// Iterate the entries (users)
		for (RosterEntry entry : entries) {

			// Validates the entry is the same to the user of the session
			if (entry.getUser().equalsIgnoreCase(user)) {
				// Looks for the specific entry
				presence = roster.getPresence(user + "@alumchat.xyz");

				// Validates if it's available or not to show contact's information
				if (presence.getType() == Presence.Type.available) {
					System.out.println("INFO: DISPONIBLE " + entry.getUser() + " - " + presence.getType() + " - "
							+ entry.getName());
				} else {
					System.out.println("INFO: DESACTIVADO " + entry.getUser() + " - " + presence.getType() + " - "
							+ entry.getName());
				}

				flag = 1;
			}
		}

		// If user does not exists
		if (flag == 0) {
			System.out.println("USUARIO NO EXISTE");
		}
	}

	/**
	 * Send file to specific user
	 * 
	 * @throws XMPPException
	 */
	public void sendFile(String fileURL, String user, String message) throws XMPPException {
		// Create the file transfer manager
		FileTransferManager manager = new FileTransferManager(connection);
		// Create the outgoing file transfer
		// OutgoingFileTransfer transfer =
		// manager.createOutgoingFileTransfer("amanda1@alumchat.xyz");
		OutgoingFileTransfer transfer = manager.createOutgoingFileTransfer(user);
		// Send the file
		// File file = new File("C:\\Users\\User\\Desktop\\Test.txt");
		File file = new File(fileURL);
		transfer.sendFile(file, message);
		while (!transfer.isDone()) {
			if (transfer.getStatus().equals(Status.error)) {
				System.out.println("ERROR!!! " + transfer.getError());
			} else {
				System.out.println(transfer.getStatus());
				System.out.println(transfer.getProgress());
			}
		}

		System.out.println("Success");
	}

	/**
	 * Receive file from user
	 */
	public void recibirArchivo() {
		// Create the file transfer manager
		FileTransferManager manager = new FileTransferManager(connection);
		// Add the file transfer listener
		manager.addFileTransferListener(new FileTransferListener() {
			// Create the file transfer request
			public void fileTransferRequest(final FileTransferRequest request) {
				new Thread() {
					// waits for the incoming file transfer and receive the file
					@Override
					public void run() {
						IncomingFileTransfer transfer = request.accept();
						String home = System.getProperty("user.home");
						File file = new File(home + "/Downloads/" + transfer.getFileName());
						try {
							transfer.recieveFile(file);
							while (!transfer.isDone()) {
								try {
									Thread.sleep(1000L);
								} catch (Exception e) {
									System.out.println("Error: " + e.getMessage());
								}
								if (transfer.getStatus().equals(Status.error)) {
									System.out.println("Error: " + transfer.getError());
								}
								if (transfer.getException() != null) {
									transfer.getException().printStackTrace();
								}
							}
						} catch (Exception e) {
							System.out.println("Error: " + e.getMessage());
						}
					};
				}.start();
			}
		});
	}

	/**
	 * Creates a roster group and add all users in roster to it
	 * 
	 * @param userName
	 * @param groupName
	 */
	public void addUserToGroup(String userName, String groupName) {
		// Generate the roster with the current connection
		Roster roster = connection.getRoster();
		// Searchs for a group if it does not exists creates it
		RosterGroup group = roster.getGroup(groupName);
		if (group == null) {
			System.out.println("Crea grupo");
			group = roster.createGroup(groupName);
		}
		// Search entries and add them if they are not in the group
		RosterEntry entry = roster.getEntry(userName);
		if (entry != null) {
			try {
				System.out.println("Agrega entrada");
				group.addEntry(entry);
			} catch (XMPPException e) {
				e.printStackTrace();
			}
		}

	}

	/**
	 * Creates a multiuser chat
	 * 
	 * @param groupId
	 * @param groupName
	 * @param nickname
	 * @throws XMPPException
	 */
	public void groupChatCreate(String groupId, String groupName, String nickname) throws XMPPException {
		// Initialize the multiUserChat
		MultiUserChat muc = new MultiUserChat(connection, groupId + "@alumchat.xyz");
		// Create the multiUserChat
		muc.create(nickname);
		Form form = muc.getConfigurationForm();
		Form submitForm = form.createAnswerForm();
		for (Iterator<FormField> fields = form.getFields(); fields.hasNext();) {
			FormField field = (FormField) fields.next();
			if (!FormField.TYPE_HIDDEN.equals(field.getType()) && field.getVariable() != null) {
				submitForm.setDefaultAnswer(field.getVariable());
			}
		}
		// Set owners and configurations of the room
		List<String> owners = new ArrayList<String>();
		owners.add(connection.getUser().toString());
		submitForm.setAnswer("muc#roomconfig_roomowners", owners);
		submitForm.setAnswer("muc#roomconfig_persistentroom", true);
		submitForm.setAnswer("muc#roomconfig_roomdesc", groupName);
		muc.sendConfigurationForm(submitForm);

	}

	public class GroupChat {

		private XMPPConnection connection;
		private String room;
		private String nickname = null;
		private boolean joined = false;
		private List<String> participants = new ArrayList<String>();

		private PacketFilter presenceFilter;
		private PacketFilter messageFilter;
		private PacketCollector messageCollector;

		/**
		 * Creates a new group chat with the specified connection and room name. Note:
		 * no information is sent to or received from the server until you attempt to
		 * {@link #join(String) join} the chat room. On some server implementations, the
		 * room will not be created until the first person joins it.
		 * <p>
		 *
		 * Most XMPP servers use a sub-domain for the chat service (eg chat.example.com
		 * for the XMPP server example.com). You must ensure that the room address
		 * you're trying to connect to includes the proper chat sub-domain.
		 *
		 * @param connection the XMPP connection.
		 * @param room       the name of the room in the form "roomName@service", where
		 *                   "service" is the hostname at which the multi-user chat
		 *                   service is running.
		 */
		public GroupChat(XMPPConnection connection, String room) {
			this.connection = connection;
			this.room = room;
			System.out.println("Room " + room);
			// Create a collector for all incoming messages.
			messageFilter = new AndFilter(new FromContainsFilter(room), new PacketTypeFilter(Message.class));
			messageFilter = new AndFilter(messageFilter, new PacketFilter() {
				public boolean accept(Packet packet) {
					Message msg = (Message) packet;
					return msg.getType() == Message.Type.groupchat;
				}
			});
			messageCollector = connection.createPacketCollector(messageFilter);
			// Create a listener for all presence updates.
			presenceFilter = new AndFilter(new FromContainsFilter(room), new PacketTypeFilter(Presence.class));
			connection.addPacketListener(new PacketListener() {
				public void processPacket(Packet packet) {
					Presence presence = (Presence) packet;
					String from = presence.getFrom();
					System.out.println("Presence " + from);
					if (presence.getType() == Presence.Type.available) {
						synchronized (participants) {
							if (!participants.contains(from)) {
								participants.add(from);
							}
						}
					} else if (presence.getType() == Presence.Type.unavailable) {
						synchronized (participants) {
							participants.remove(from);
						}
					}
				}
			}, presenceFilter);
		}

		/**
		 * Returns the name of the room this GroupChat object represents.
		 *
		 * @return the groupchat room name.
		 */
		public String getRoom() {
			return room;
		}

		/**
		 * Joins the chat room using the specified nickname. If already joined using
		 * another nickname, this method will first leave the room and then re-join
		 * using the new nickname. The default timeout of 5 seconds for a reply from the
		 * group chat server that the join succeeded will be used.
		 *
		 * @param nickname the nickname to use.
		 * @throws XMPPException if an error occurs joining the room. In particular, a
		 *                       409 error can occur if someone is already in the group
		 *                       chat with the same nickname.
		 */
		public synchronized void join(String nickname) throws XMPPException {
			join(nickname, SmackConfiguration.getPacketReplyTimeout());
		}

		/**
		 * Joins the chat room using the specified nickname. If already joined as
		 * another nickname, will leave as that name first before joining under the new
		 * name.
		 *
		 * @param nickname the nickname to use.
		 * @param timeout  the number of milleseconds to wait for a reply from the group
		 *                 chat that joining the room succeeded.
		 * @throws XMPPException if an error occurs joining the room. In particular, a
		 *                       409 error can occur if someone is already in the group
		 *                       chat with the same nickname.
		 */
		public synchronized void join(String nickname, long timeout) throws XMPPException {
			if (nickname == null || nickname.equals("")) {
				throw new IllegalArgumentException("Nickname must not be null or blank.");
			}
			// If we've already joined the room, leave it before joining under a new
			// nickname.
			if (joined) {
				leave();
			}
			// We join a room by sending a presence packet where the "to"
			// field is in the form "roomName@service/nickname"
			Presence joinPresence = new Presence(Presence.Type.available);
			System.out.println(room + "@alumchat.xyz/" + nickname);
			joinPresence.setTo(room + "@alumchat.xyz/" + nickname);
			// Wait for a presence packet back from the server.
			PacketFilter responseFilter = new AndFilter(new FromContainsFilter(room + "/" + nickname),
					new PacketTypeFilter(Presence.class));
			PacketCollector response = connection.createPacketCollector(responseFilter);
			// Send join packet.
			connection.sendPacket(joinPresence);
			// Wait up to a certain number of seconds for a reply.
			Presence presence = (Presence) response.nextResult(timeout);
			response.cancel();
			if (presence == null) {
				throw new XMPPException("No response from server.");
			} else if (presence.getError() != null) {
				throw new XMPPException(presence.getError());
			}
			this.nickname = nickname;
			joined = true;
		}

		/**
		 * Returns true if currently in the group chat (after calling the
		 * {@link #join(String)} method.
		 *
		 * @return true if currently in the group chat room.
		 */
		public boolean isJoined() {
			return joined;
		}

		/**
		 * Leave the chat room.
		 */
		public synchronized void leave() {
			// If not joined already, do nothing.
			if (!joined) {
				return;
			}
			// We leave a room by sending a presence packet where the "to"
			// field is in the form "roomName@service/nickname"
			Presence leavePresence = new Presence(Presence.Type.unavailable);
			leavePresence.setTo(room + "/" + nickname);
			connection.sendPacket(leavePresence);
			// Reset participant information.
			participants = new ArrayList<String>();
			nickname = null;
			joined = false;
		}

		/**
		 * Returns the nickname that was used to join the room, or <tt>null if not
		 * currently joined.
		 *
		 * @return the nickname currently being used.
		 */
		public String getNickname() {
			return nickname;
		}

		/**
		 * Returns the number of participants in the group chat.
		 * <p>
		 *
		 * Note: this value will only be accurate after joining the group chat, and may
		 * fluctuate over time. If you query this value directly after joining the group
		 * chat it may not be accurate, as it takes a certain amount of time for the
		 * server to send all presence packets to this client.
		 *
		 * @return the number of participants in the group chat.
		 */
		public int getParticipantCount() {
			synchronized (participants) {
				return participants.size();
			}
		}

		/**
		 * Returns an Iterator (of Strings) for the list of fully qualified participants
		 * in the group chat. For example, "conference@chat.jivesoftware.com/SomeUser".
		 * Typically, a client would only display the nickname of the participant. To
		 * get the nickname from the fully qualified name, use the
		 * {@link org.jivesoftware.smack.util.StringUtils#parseResource(String)} method.
		 * Note: this value will only be accurate after joining the group chat, and may
		 * fluctuate over time.
		 *
		 * @return an Iterator for the participants in the group chat.
		 */
		public Iterator<String> getParticipants() {
			synchronized (participants) {
				return Collections.unmodifiableList(new ArrayList<String>(participants)).iterator();
			}
		}

		/**
		 * Adds a packet listener that will be notified of any new Presence packets sent
		 * to the group chat. Using a listener is a suitable way to know when the list
		 * of participants should be re-loaded due to any changes.
		 *
		 * @param listener a packet listener that will be notified of any presence
		 *                 packets sent to the group chat.
		 */
		public void addParticipantListener(PacketListener listener) {
			connection.addPacketListener(listener, presenceFilter);
		}

		/**
		 * Sends a message to the chat room.
		 *
		 * @param text the text of the message to send.
		 * @throws XMPPException if sending the message fails.
		 */
		public void sendMessage(String text) throws XMPPException {
			Message message = new Message(room, Message.Type.groupchat);
			message.setBody(text);
			connection.sendPacket(message);
		}

		/**
		 * Creates a new Message to send to the chat room.
		 *
		 * @return a new Message addressed to the chat room.
		 */
		public Message createMessage() {
			return new Message(room, Message.Type.groupchat);
		}

		/**
		 * Sends a Message to the chat room.
		 *
		 * @param message the message.
		 * @throws XMPPException if sending the message fails.
		 */
		public void sendMessage(Message message) throws XMPPException {
			connection.sendPacket(message);
		}

		/**
		 * Polls for and returns the next message, or <tt>null if there isn't a message
		 * immediately available. This method provides significantly different
		 * functionalty than the {@link #nextMessage()} method since it's non-blocking.
		 * In other words, the method call will always return immediately, whereas the
		 * nextMessage method will return only when a message is available (or after a
		 * specific timeout).
		 *
		 * @return the next message if one is immediately available and <tt>null
		 *         otherwise.
		 */
		public Message pollMessage() {
			return (Message) messageCollector.pollResult();
		}

		/**
		 * Returns the next available message in the chat. The method call will block
		 * (not return) until a message is available.
		 *
		 * @return the next message.
		 */
		public Message nextMessage() {
			return (Message) messageCollector.nextResult();
		}

		/**
		 * Returns the next available message in the chat. The method call will block
		 * (not return) until a packet is available or the <tt>timeout has elapased. If
		 * the timeout elapses without a result, <tt>null will be returned.
		 *
		 * @param timeout the maximum amount of time to wait for the next message.
		 * @return the next message, or <tt>null if the timeout elapses without a
		 *         message becoming available.
		 */
		public Message nextMessage(long timeout) {
			return (Message) messageCollector.nextResult(timeout);
		}

		/**
		 * Adds a packet listener that will be notified of any new messages in the group
		 * chat. Only "group chat" messages addressed to this group chat will be
		 * delivered to the listener. If you wish to listen for other packets that may
		 * be associated with this group chat, you should register a PacketListener
		 * directly with the XMPPConnection with the appropriate PacketListener.
		 *
		 * @param listener a packet listener.
		 */
		public void addMessageListener(PacketListener listener) {
			connection.addPacketListener(listener, messageFilter);
		}

		public void finalize() throws Throwable {
			super.finalize();
			try {
				if (messageCollector != null) {
					messageCollector.cancel();
				}
			} catch (Exception e) {
			}
		}
	}

}