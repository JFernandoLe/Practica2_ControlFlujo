import javax.swing.*;
import java.net.*;
import java.io.*;
import java.nio.file.Path;
import java.util.Arrays;

/**
 *
 * @author axele
 */
public class Cliente {

    public static void main(String[] args) {
        try {
            int pto = 1234;
            String dir = "127.0.0.1";
            InetAddress dst = InetAddress.getByName(dir);
            int tam = 10, x = 0;
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            DatagramSocket cl = new DatagramSocket();
            while (true) {
                x = 0;
                JFileChooser jf=new JFileChooser();
                int r=jf.showOpenDialog(null);
                jf.setRequestFocusEnabled(true);
                if(r==JFileChooser.APPROVE_OPTION){
                    File f=jf.getSelectedFile();
                    String nombre=f.getName();
                    String path=f.getAbsolutePath();
                    long size=f.length();
                    System.out.println("Preparandose para enviar archivo "+path+" de "+tam+" bytes\n\n");
                    DataInputStream dis=new DataInputStream(new FileInputStream(path));
                    if (size > tam) {

                        int tp = (int) (size / tam);

                        for (int j = 0; j < tp; j++) {

                            x++; //Nuestro contador
                            byte[] tmp = Arrays.copyOfRange(b, j * tam, ((j * tam) + (tam)));
                            //Creamos un nuevo arreglo que contendrá el número de paquete
                            byte[] tmpConContador=new byte[tmp.length+1];
                            System.arraycopy(tmp, 0, tmpConContador,0,tmp.length);
                            tmpConContador[tmp.length]=(byte)x;

                            //System.out.println("Paquete:"+x+", bytes enviados: "+tmp.length);
                            DatagramPacket p = new DatagramPacket(tmpConContador, tmpConContador.length, dst, pto);
                            cl.send(p);
                            System.out.println("Enviando fragmento " + x + "\ndesde:" + (j * tam) + " hasta " + ((j * tam) + (tam - 1)));
                            DatagramPacket p1 = new DatagramPacket(new byte[tam], tam);
                            cl.receive(p1);
                            byte[] bp1 = p1.getData();
                            for (int i = 0; i < tam; i++) {
                                //System.out.println((j*tam)+i+"->"+tmp[i]);
                                b_eco[(j * tam) + i] = bp1[i];
                            }//for
                        }//for
                        if (b.length % tam > 0) { //bytes sobrantes
                            //tp=tp+1;
                            x++;
                            int sobrantes = b.length % tam;
                            System.out.println("sobrantes:" + sobrantes);
                            //System.out.println("paquete: " + x + "  b:" + b.length + "ultimo pedazo desde " + tp * tam + " hasta " + ((tp * tam) + sobrantes - 1));
                            byte[] tmp = Arrays.copyOfRange(b, tp * tam, ((tp * tam) + sobrantes));
                            //Creamos un nuevo arreglo que contendrá el número de paquete
                            byte[] tmpConContador=new byte[tmp.length+1];
                            System.arraycopy(tmp, 0, tmpConContador,0,tmp.length);
                            tmpConContador[tmp.length]=(byte)x;
                            //System.out.println("tmp tam "+tmp.length);
                            DatagramPacket p = new DatagramPacket(tmpConContador, tmpConContador.length, dst, pto);
                            cl.send(p);
                            DatagramPacket p1 = new DatagramPacket(new byte[tam], tam);
                            cl.receive(p1);
                            byte[] bp1 = p1.getData();
                            for (int i = 0; i < sobrantes; i++) {
                                // System.out.println((tp*tam)+i+"->"+i);
                                b_eco[(tp * tam) + i] = bp1[i];
                            }//for
                        }//if

                        String eco = new String(b_eco);
                        System.out.println("Eco recibido: " + eco);
                    } else {
                        DatagramPacket p = new DatagramPacket(b, b.length, dst, pto);
                        cl.send(p);
                        DatagramPacket p1 = new DatagramPacket(new byte[65535], 65535);
                        cl.receive(p1);
                        String eco = new String(p1.getData(), 0, p1.getLength());
                        System.out.println("Eco recibido: " + eco);
                    }//else

                }


            }//while
        } catch (Exception e) {
            e.printStackTrace();
        }//catch
    }//main
}
