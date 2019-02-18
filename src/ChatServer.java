import authorization.AuthService;
import authorization.AuthServiceImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Java. Level 2. Lesson 8.
  * @version 17.02.2019
 */

/*
Lesson 7 part 2
1. Разобраться с кодом.
2. *Реализовать личные сообщения так: если клиент пишет «/w nick3 Привет»,
то только клиенту с ником nick3 должно прийти сообщение «Привет».
3. Решить TO DO

Что сделано:
1. Разобрался с кодом
2. Использовал Ваш шаблон и добавил распознавание строки вида:
"/w ivan текст".
3. Добавил отправку сообщения по клавише "ENTER"
4. Сделал так, чтобы по мере подключений/отключений пользователей в левой колонке
 был актуальный список пользователей.
*/



public class ChatServer {

    //Объявляем паттерны

    private static final Pattern AUTH_PATTERN = Pattern.compile("^/auth (\\w+) (\\w+)$");


    private AuthService authService = new AuthServiceImpl();

    private Map<String, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());
    public static void main(String[] args) {
        ChatServer chatServer = new ChatServer();
        chatServer.start(7777);
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started!");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.println("New client connected!");


                try {
                    String authMessage = inp.readUTF();
                    Matcher matcher = AUTH_PATTERN.matcher(authMessage);
                    if (matcher.matches()) {
                        String username = matcher.group(1);
                        String password = matcher.group(2);

                        if (authService.authUser(username, password)) {
                            clientHandlerMap.put(username, new ClientHandler(username, socket, this));
                            out.writeUTF("/auth successful");
                            out.flush();
                            System.out.printf("Authorization for user %s successful%n", username);



                            //Отправляем пользователя
                            broadcastUserConnection();

                            //out.writeUTF(USER_CONNECTED_PATTERN + username);
                            //out.flush();


                        } else {
                            System.out.printf("Authorization for user %s failed%n", username);

                            out.writeUTF("/auth fails");
                            out.flush();
                            socket.close();
                        }
                    } else {
                        System.out.printf("Incorrect authorization message %s%n", authMessage);
                        out.writeUTF("/auth fails");
                                      out.flush();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String userTo, String userFrom, String msg) throws IOException {
        ClientHandler userToClientHandler = clientHandlerMap.get(userTo);
        if (userToClientHandler != null) {
            userToClientHandler.sendMessage(userFrom, msg);
        } else {
            System.out.printf("User %s not found. Message from %s is lost.%n", userTo, userFrom);
        }
    }

    public void sendUserConsistMessage(String userTo, String userFrom, String msg) throws IOException {
        ClientHandler userToClientHandler = clientHandlerMap.get(userTo);
        if (userToClientHandler != null) {
            userToClientHandler.sendUserConsistMessage(userFrom, msg);
        } else {
            System.out.printf("User %s not found. Message from %s is lost.%n", userTo, userFrom);
        }
    }




    public List<String> getUserList() {
        return new ArrayList<>(clientHandlerMap.keySet());
    }

    public void unsubscribeClient(ClientHandler clientHandler) throws IOException {


       //Удаляем из списка
        clientHandlerMap.remove(clientHandler.getUsername());
        broadcastUserConnection();

    }

    //Шлем информацию всем клиентам при подключении/отключении пользователя
    public void broadcastUserConnection() {

        System.out.println(getUserList());
        List<String> namesList;
        namesList = getUserList();

        //Преобразуем ArrayList в строку для пересылки
        StringBuilder sb = new StringBuilder();
        for (String s :  namesList)
        {
            sb.append(s);
            sb.append("//");
        }


        //Шлем всем информацию о текущих пользователях
        for (String name : namesList) {
            try {
                 sendUserConsistMessage(name,"", sb.toString());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }




}
