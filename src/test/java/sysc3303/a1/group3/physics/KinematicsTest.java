// package sysc3303.a1.group3.physics;

// import org.junit.jupiter.api.Test;

// import static org.junit.jupiter.api.Assertions.assertEquals;

// import static org.junit.jupiter.api.Assertions.assertFalse;
// import static sysc3303.a1.group3.physics.Kinematics.EPSILON;
// import static sysc3303.a1.group3.physics.Kinematics.timeToReachSpeed;

// public class KinematicsTest {

//     @Test
//     void testTimeToReachSpeedRest() {
//         Vector2d v = Vector2d.ZERO; // starting at rest
//         Vector2d a = Vector2d.of(3, 4);

//         // 5 = magnitude(3, 4)
//         double time1 = timeToReachSpeed(v, a, 5);
//         double time2 = timeToReachSpeed(v, a, 10);

//         assertEquals(1, time1, EPSILON);
//         assertEquals(2, time2, EPSILON);
//     }

//     @Test
//     void testTimeToReachSpeedTrivial() {
//         Vector2d v = Vector2d.of(5, 12);
//         Vector2d a = Vector2d.ZERO;

//         double time = timeToReachSpeed(v, a, 13);
//         assertEquals(0, time, EPSILON);
//     }

//     @Test
//     void testTimeToReachSpeed() {
//         Vector2d v = Vector2d.of(2, 6);
//         Vector2d a = Vector2d.of(2, 4);

//         // (2, 4) * 1.5 = (3, 6)
//         // (3, 6) + (2, 6) = (5, 12)
//         // magnitude(5, 12) = 13

//         double time = timeToReachSpeed(v, a, 13);
//         assertEquals(1.5, time, EPSILON);
//     }

//     @Test
//     void voidTestTimeToReachSpeedEdge() {
//         // v = Vector2d[x=3.313083248978657, y=9.435225454928835]
//         // a = Vector2d[x=8.766549373974314, y=28.69054917692707]
//         // speed = 10.0

//         // This was resulting in an error
//         Vector2d v = Vector2d.of(3.313083248978657, 9.435225454928835);
//         Vector2d a = Vector2d.of(8.766549373974314, 28.69054917692707);

//         assertFalse(Double.isNaN(timeToReachSpeed(v, a, 10)));
//     }
// }
