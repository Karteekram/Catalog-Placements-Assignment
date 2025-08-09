// Save as SecretPolynomialSolver.java
import java.io.FileReader;
import java.math.BigInteger;
import java.util.*;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

/**
 * Reads input.json in same folder, decodes values from given base,
 * picks first k points (by numeric key order) and computes f(0) using
 * Lagrange interpolation. Uses exact rational arithmetic via Fraction.
 */
public class SecretPolynomialSolver {

    // Simple rational number with BigInteger numerator/denominator
    static class Fraction {
        BigInteger num;
        BigInteger den;

        Fraction(BigInteger n, BigInteger d) {
            if (d.signum() == 0) throw new ArithmeticException("zero denominator");
            if (d.signum() < 0) { n = n.negate(); d = d.negate(); }
            BigInteger g = n.gcd(d);
            num = n.divide(g);
            den = d.divide(g);
        }

        static Fraction zero() { return new Fraction(BigInteger.ZERO, BigInteger.ONE); }
        static Fraction fromBigInt(BigInteger x) { return new Fraction(x, BigInteger.ONE); }

        Fraction add(Fraction o) {
            BigInteger n = num.multiply(o.den).add(o.num.multiply(den));
            BigInteger d = den.multiply(o.den);
            return new Fraction(n, d);
        }

        Fraction mul(Fraction o) {
            return new Fraction(num.multiply(o.num), den.multiply(o.den));
        }

        Fraction div(Fraction o) {
            if (o.num.signum() == 0) throw new ArithmeticException("divide by zero fraction");
            return new Fraction(num.multiply(o.den), den.multiply(o.num));
        }

        Fraction negate() { return new Fraction(num.negate(), den); }

        @Override
        public String toString() {
            if (den.equals(BigInteger.ONE)) return num.toString();
            return num.toString() + "/" + den.toString();
        }

        // Return integer if exact, else throws
        BigInteger toBigIntegerExact() {
            BigInteger[] qr = num.divideAndRemainder(den);
            if (!qr[1].equals(BigInteger.ZERO)) throw new ArithmeticException("Not an integer");
            return qr[0];
        }
    }

    // Lagrange interpolation at x=0 using rational arithmetic
    public static Fraction lagrangeAtZero(List<BigInteger[]> points) {
        int k = points.size();
        Fraction result = Fraction.zero();

        for (int i = 0; i < k; i++) {
            BigInteger xi = points.get(i)[0];
            BigInteger yi = points.get(i)[1];

            Fraction term = Fraction.fromBigInt(yi); // start with y_i
            for (int j = 0; j < k; j++) {
                if (i == j) continue;
                BigInteger xj = points.get(j)[0];

                // multiply by (0 - xj) / (xi - xj)
                Fraction numerator = Fraction.fromBigInt(xj.negate()); // (0 - xj)
                Fraction denominator = Fraction.fromBigInt(xi.subtract(xj)); // (xi - xj)
                term = term.mul(numerator).div(denominator);
            }
            result = result.add(term);
        }
        return result;
    }

    public static void main(String[] args) {
        try {
            // Read JSON file named "input.json" in current directory
            JSONParser parser = new JSONParser();
            JSONObject jsonObject = (JSONObject) parser.parse(new FileReader("input.json"));

            JSONObject keysObj = (JSONObject) jsonObject.get("keys");
            if (keysObj == null) {
                System.err.println("Missing 'keys' object in input.json");
                return;
            }
            int n = Integer.parseInt(keysObj.get("n").toString());
            int k = Integer.parseInt(keysObj.get("k").toString());
            if (k <= 0) {
                System.err.println("Invalid k in keys");
                return;
            }

            // Collect numeric keys present in the top-level JSON (exclude "keys")
            List<Integer> presentKeys = new ArrayList<>();
            for (Object keyObj : jsonObject.keySet()) {
                String key = keyObj.toString();
                if (key.equals("keys")) continue;
                try {
                    int ki = Integer.parseInt(key);
                    presentKeys.add(ki);
                } catch (NumberFormatException ex) {
                    // ignore non-numeric keys
                }
            }
            Collections.sort(presentKeys);

            // We will use the first k numeric keys (in ascending order).
            if (presentKeys.size() < k) {
                System.err.println("Not enough points in JSON to pick k points.");
                return;
            }

            List<BigInteger[]> points = new ArrayList<>();
            for (int idx = 0; idx < k; idx++) {
                int key = presentKeys.get(idx);
                JSONObject pointObj = (JSONObject) jsonObject.get(String.valueOf(key));
                if (pointObj == null) {
                    System.err.println("Missing object for key: " + key);
                    return;
                }
                int base = Integer.parseInt(pointObj.get("base").toString());
                String valueStr = pointObj.get("value").toString().toLowerCase(); // allow a-f

                // decode y using BigInteger(valueStr, base)
                BigInteger y = new BigInteger(valueStr, base);
                BigInteger x = BigInteger.valueOf(key);

                points.add(new BigInteger[]{x, y});
            }

            // Compute f(0)
            Fraction secretFraction = lagrangeAtZero(points);
            System.out.println("f(0) as rational = " + secretFraction);

            try {
                BigInteger secret = secretFraction.toBigIntegerExact();
                System.out.println("Secret (C) = " + secret);
            } catch (ArithmeticException e) {
                System.out.println("Secret is not an integer. Rational value: " + secretFraction);
            }

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
