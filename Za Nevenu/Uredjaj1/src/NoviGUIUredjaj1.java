import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class NoviGUIUredjaj1 {
    private JPanel panel1;
    private JLabel adresa;
    private JButton dugme;

    static JFrame frame;

    ServerSocket serverSocket;

    boolean videoIde = false;


    NoviGUIUredjaj1(){

        try {
            InetAddress localhost = InetAddress.getLocalHost();
            String ipAddress = localhost.getHostAddress();

            adresa.setText("Vasa adresa: " + ipAddress);

        } catch (Exception e) {
            e.printStackTrace();
        }

        dugme.addActionListener(e -> {
            SwingWorker<Void, Void> worker = new SwingWorker<>() {
                @Override
                protected Void doInBackground() {

                    if(videoIde){
                        resetuj(2);
                    }else {
                        videoIde= true;
                        pokreniUredjaj();
                    }
                    return null;
                }
            };
            worker.execute();
        });

    }

    private void pokreniUredjaj(){
        try {
            serverSocket = new ServerSocket(12345);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

            try {
                dugme.setEnabled(false);
                dugme.setText("Cekam da se poveze klijent.");
                Socket klijentSocket = serverSocket.accept();
                dugme.setEnabled(true);
                dugme.setText("Prekini prenos.");
                DataOutputStream dos = new DataOutputStream(klijentSocket.getOutputStream());
                DataInputStream dis = new DataInputStream(klijentSocket.getInputStream());

                System.out.println("Kreirani input i output streamovi na serveru");

                new Thread(() -> {
                    while (true){
                        if(!klijentSocket.isConnected()){
                            resetuj(1);
                            break;
                        }
                    }
                }).start();

                new Thread(() -> {
                    try {
                        Robot robot = new Robot();
                        while (videoIde) {
                            int eventType = dis.readInt();

                            //ako se radi o kliku misa
                            if (eventType == 0) {
                                int x = dis.readInt();
                                int y = dis.readInt();
                                int buttonType = dis.readInt();
                                System.out.println("primljen klik");
                                simulateMouseClick(x, y, buttonType);
                            }
                            //ako se radi o tastaturi
                            else if (eventType == 1) {
                                int keyCode = dis.readInt();
                                robot.keyPress(keyCode);
                                robot.keyRelease(keyCode);

                            }
                        }
                    } catch (IOException ex) {
                        //throw new RuntimeException(ex);
                        //
                    } catch (AWTException ex) {
                        throw new RuntimeException(ex);
                    }

                }).start();

                //Slanje dimenzija servera
                Dimension size = Toolkit.getDefaultToolkit().getScreenSize();
                int width = size.width;
                int height = size.height;

                dos.writeInt(width);
                dos.writeInt(height);
                dos.flush();

                // Slanje slike klijentu
                while (videoIde) {
                    System.out.println("Salje se slika klijentu!");
                    byte[] niz = pretvoriSlikuUBajtove(napraviScreenshot());
                    dos.writeInt(niz.length);  // šalje se dužina niza sa slikom
                    dos.write(niz);            // šalje se slika
                    dos.flush();

                    Thread.sleep(50);
                }
                dos.close();
                klijentSocket.close();
                //dugme.setEnabled(true);
                //videoIde = true;
            } catch (Exception ex) {
                //ex.printStackTrace();
                resetuj(1);
            }
        }


    public static void main(String[] args) {
        pokreniInterfejs();
    }

    private static void pokreniInterfejs() {
        frame = new JFrame("KlijentGUI");
        NoviGUIUredjaj1 uredjajGUI = new NoviGUIUredjaj1();
        Dimension minimumSize = new Dimension(500, 250); // Proporcije 16:9
        frame.setMinimumSize(minimumSize);
        frame.setContentPane(uredjajGUI.panel1);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.pack();
        frame.setVisible(true);

    }

    private void simulateMouseClick(int x, int y, int buttonType) {
        try {
            Robot robot = new Robot();
            robot.mouseMove(x, y);

            int buttonDownMask = 0;
            switch (buttonType) {
                case MouseEvent.BUTTON1:  // Levo dugme miša
                    buttonDownMask = InputEvent.BUTTON1_DOWN_MASK;
                    break;
                case MouseEvent.BUTTON2:  // Srednje dugme miša
                    buttonDownMask = InputEvent.BUTTON2_DOWN_MASK;
                    break;
                case MouseEvent.BUTTON3:  // Desno dugme miša
                    buttonDownMask = InputEvent.BUTTON3_DOWN_MASK;
                    break;
            }

            System.out.println("Simuliran klik");

            robot.mousePress(buttonDownMask);
            robot.mouseRelease(buttonDownMask);
        } catch (AWTException e) {
            e.printStackTrace();
        }
    }

    // Pretvaranje BufferedImage u niz bajtova
    private byte[] pretvoriSlikuUBajtove(BufferedImage slika) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            ImageIO.write(slika, "png", baos);
        } catch (IOException e) {
            System.out.println("greksa prilikom pretvaranja slike u bajtove");
        }
        byte[] imageData = baos.toByteArray();
        return imageData;
    }

    // Pravljenje screenshot-a trenutnog ekrana
    private BufferedImage napraviScreenshot() {
        Robot robot = null;
        BufferedImage screenshot;
        try {
            robot = new Robot();
            Rectangle screenRect = new Rectangle(Toolkit.getDefaultToolkit().getScreenSize());
            screenshot = robot.createScreenCapture(screenRect);
            return screenshot;
        } catch (AWTException e) {
            System.out.println("Greska prilikom pravljenja screenshot-a.");
        }
        return null;
    }

    public void resetuj(int broj){
        videoIde=false;
        frame.dispose();
        switch (broj){
            case 1:
                JOptionPane.showMessageDialog(null,"Klijent se odvezao.");
                break;
            case 2:
                JOptionPane.showMessageDialog(null,"Prekinuli ste prenos.");
                break;
        }

        try {
            serverSocket.close();
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
        pokreniInterfejs();
    }




}
