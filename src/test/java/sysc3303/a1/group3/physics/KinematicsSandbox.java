// package sysc3303.a1.group3.physics;

// import javax.swing.JFrame;
// import javax.swing.JPanel;
// import javax.swing.SwingUtilities;
// import java.awt.Color;
// import java.awt.Container;
// import java.awt.Dimension;
// import java.awt.Graphics;
// import java.awt.MouseInfo;
// import java.awt.Point;
// import java.util.ArrayDeque;
// import java.util.Queue;

// class KinematicsSandbox extends JPanel {

//     private static final int MAX_POINTS_DRAWN = 10;

//     private final Kinematics kin;

//     private final double graphScale = 20;

//     private SpeedPoint newestPoint = null;
//     private final Queue<SpeedPoint> oldPoints = new ArrayDeque<>();

//     public KinematicsSandbox(Kinematics kinematics) {
//         this.kin = kinematics;
//     }

//     public Vector2d getCursorPosition() {
//         Point mouse = MouseInfo.getPointerInfo().getLocation();
//         SwingUtilities.convertPointFromScreen(mouse, this);
//         return Vector2d.of(mouse.x / graphScale, mouse.y / graphScale);
//     }

//     public void update() {
//         SpeedPoint point = new SpeedPoint(kin.getPosition(), kin.getVelocity(), kin.getLastAcceleration());

//         // Replace newestPoint
//         if (newestPoint != null) {
//             oldPoints.add(newestPoint);
//         }
//         newestPoint = point;

//         while (oldPoints.size() > MAX_POINTS_DRAWN) {
//             oldPoints.remove();
//         }

//         repaint();
//     }

//     @Override
//     protected void paintComponent(Graphics g) {
//         super.paintComponent(g);

//         // target
//         drawPoint(g, kin.getTarget(), Color.BLACK);

//         if (newestPoint != null) {
//             drawSpeedPoint(g, newestPoint, true);
//         }

//         // speed points
//         for (SpeedPoint point : oldPoints) {
//             drawSpeedPoint(g, point, false);
//         }
//     }

//     private void drawPoint(Graphics g, Vector2d position, Color color) {
//         Color oldColor = g.getColor();

//         int x = (int) (position.getX() * graphScale);
//         int y = (int) (position.getY() * graphScale);
//         g.setColor(color);
//         g.fillOval(x, y, 5, 5);

//         g.setColor(oldColor);
//     }

//     @SuppressWarnings("SameParameterValue")
//     private void drawLine(Graphics g, Vector2d start, Vector2d line, Color color) {
//         Color oldColor = g.getColor();

//         int x = (int) (start.getX() * graphScale);
//         int y = (int) (start.getY() * graphScale);
//         int a = (int) (line.getX() * graphScale);
//         int b = (int) (line.getY() * graphScale);

//         g.setColor(color);
//         g.drawLine(x, y, x + a, y + b);

//         g.setColor(oldColor);
//     }

//     private void drawSpeedPoint(Graphics g, SpeedPoint speedPoint, boolean drawAcceleration) {
//         Vector2d position = speedPoint.position();

//         double speedRatio = speedPoint.velocity().magnitude() / kin.getMaxSpeed();
//         int green = (int) (speedRatio * 255);
//         drawPoint(g, position, new Color(0, green, 0));

//         if (drawAcceleration) {
//             Vector2d acceleration = speedPoint.acceleration();
//             // Check to avoid drawing, but also because you can't normalize the zero-vector
//             if (!acceleration.equals(Vector2d.ZERO)) {
//                 drawLine(g, position, speedPoint.acceleration().withMagnitude(1), Color.RED);
//             }
//         }
//     }

//     record SpeedPoint(Vector2d position, Vector2d velocity, Vector2d acceleration) {

//     }

//     public static void main(String[] args) throws Exception {
// //        Kinematics kinematics = new Kinematics(10, 30);
// //
// //        kinematics.setTarget(Vector2d.of(10, 10));
// //
// //
// //        JFrame frame = new JFrame();
// //        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
// //        Container container = frame.getContentPane();
// //
// //        KinematicsSandbox graph = new KinematicsSandbox(kinematics);
// //        container.add(graph);
// //
// //        graph.setPreferredSize(new Dimension(500, 500));
// //
// //        frame.pack();
// //        frame.setVisible(true);
// //
// //        int tickMillis = 10;
// //        double tickSeconds =  tickMillis / 1000d;
// //
// //        //noinspection InfiniteLoopStatement
// //        while (true) {
// //            kinematics.setTarget(graph.getCursorPosition()); // assume this doesn't take much time
// //
// //            kinematics.tick(tickSeconds);
// //            graph.update(); // assume this doesn't take much time
// //
// //            //noinspection BusyWait
// //            Thread.sleep(tickMillis);
// //        }
//     }
// }
