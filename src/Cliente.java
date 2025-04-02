import java.net.*;
import java.io.*;
import javax.swing.JFileChooser;

public class Cliente {
    private static final int WINDOW_SIZE = 5; //Tamaño de ventana

    public static void main(String[] args) {
        try {
            int pto = 8000;
            String dir = "127.0.0.1";
            DatagramSocket cl = new DatagramSocket();
            System.out.println("Conexión establecida.. Escoja un archivo para enviar...");
            JFileChooser jf = new JFileChooser();
            int r = jf.showOpenDialog(null);
            if (r == JFileChooser.APPROVE_OPTION) {
                File f = jf.getSelectedFile();
                String nombre = f.getName();
                String path = f.getAbsolutePath();
                long tam = f.length();
                System.out.println("Preparándose para enviar archivo " + path + " de " + tam + " bytes\n\n");

                // Enviar nombre y tamaño del archivo
                byte[] nombreBytes = nombre.getBytes(); //Convertimos el nombre del archivo en un arreglo de bytes
                DatagramPacket p = new DatagramPacket(nombreBytes, nombreBytes.length, InetAddress.getByName(dir), pto);
                cl.send(p); //Enviamos el nombre
                byte[] tamBytes = String.valueOf(tam).getBytes(); //Convertimos el tamaño del archivo en un arreglo de bytes
                p = new DatagramPacket(tamBytes, tamBytes.length, InetAddress.getByName(dir), pto);
                cl.send(p); //Enviamos el tamaño

                DataInputStream dis = new DataInputStream(new FileInputStream(path));
                long enviados = 0;
                int l = 0, porcentaje = 0, base = 0, numSeqSig = 0;
                byte[][] window = new byte[WINDOW_SIZE][]; //Es un arreglo que almacena los paquetes que se han enviado pero aún no han recibido confirmación (ACK)

                while (enviados < tam) {
                    while (numSeqSig < base + WINDOW_SIZE && enviados < tam) {

                        byte[] b = new byte[1500];
                        l = dis.read(b);
                        if (l == -1) break; // Cuando l es igual a -1, significa que ya se terminó de leer el archivo
                        // Crear un arreglo de bytes que contenga el número de secuencia y los datos del archivo
                        ByteArrayOutputStream baos = new ByteArrayOutputStream();
                        DataOutputStream dos = new DataOutputStream(baos);
                        // Escribir el número de secuencia seguido de los datos del archivo
                        dos.writeInt(numSeqSig); // Enviar el número de secuencia como entero
                        dos.write(b, 0, l); // Escribir los datos del archivo
                        byte[] packetData = baos.toByteArray(); // Convertir todo a un arreglo de bytes
                        // Enviar el paquete con número de secuencia incluido
                        p = new DatagramPacket(packetData, packetData.length, InetAddress.getByName(dir), pto);
                        cl.send(p);
                        System.out.println("Paquete enviado: " + numSeqSig);
                        enviados += l;
                        numSeqSig++;
                    }
                    byte[] ack = new byte[4]; //El número de secuencia que estamos enviando o recibiendo se representa como un entero, por lo que necesitamos un arreglo de 4 bytes para almacenar ese número completo.
                    DatagramPacket ackPacket = new DatagramPacket(ack, ack.length);
                    cl.receive(ackPacket); //Nos quedamos a la espera del paquete con el acuse
                    int ackNum = new DataInputStream(new ByteArrayInputStream(ack)).readInt(); //Leemos del paquete el acuse y lo leemos como entero
                    System.out.println("ACK recibido: " + ackNum);
                    base = ackNum + 1; //Dado que ya se recibió un acuse, podemos ampliar la base de la ventana
                }

                System.out.println("\nArchivo enviado.. ");
                dis.close();
                cl.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}