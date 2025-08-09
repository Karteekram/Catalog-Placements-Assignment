// index.js
// Usage: node index.js input.json
const fs = require('fs');

function parseBaseToBigInt(str, base) {
  const b = BigInt(base);
  str = String(str).trim().toLowerCase();
  let acc = 0n;
  for (const ch of str) {
    let digit;
    if (ch >= '0' && ch <= '9') digit = BigInt(ch.charCodeAt(0) - 48);
    else if (ch >= 'a' && ch <= 'z') digit = BigInt(ch.charCodeAt(0) - 87);
    else throw new Error(`Unsupported digit character: '${ch}'`);
    if (digit >= b) throw new Error(`Digit ${digit} >= base ${base}`);
    acc = acc * b + digit;
  }
  return acc;
}

function absBig(x) { return x < 0n ? -x : x; }
function gcdBig(a, b) {
  a = absBig(a); b = absBig(b);
  while (b !== 0n) {
    const t = a % b;
    a = b;
    b = t;
  }
  return a;
}
function reduceFrac(n, d) {
  if (d === 0n) throw new Error('zero denominator');
  if (d < 0n) { n = -n; d = -d; }
  const g = gcdBig(n < 0n ? -n : n, d);
  return [n / g, d / g];
}
function fracAdd([n1, d1], [n2, d2]) {
  return reduceFrac(n1 * d2 + n2 * d1, d1 * d2);
}
function fracMul([n1, d1], [n2, d2]) {
  return reduceFrac(n1 * n2, d1 * d2);
}
function fracDiv([n1, d1], [n2, d2]) {
  if (n2 === 0n) throw new Error('division by zero in fraction');
  return reduceFrac(n1 * d2, d1 * n2);
}

function interpolateAtZero(points, k) {
  const pts = points.slice(0, k);
  let result = [0n, 1n]; // fraction 0/1
  for (let i = 0; i < k; i++) {
    const xi = pts[i].x;
    const yi = pts[i].y;
    let basis = [1n, 1n];
    for (let j = 0; j < k; j++) {
      if (j === i) continue;
      const xj = pts[j].x;
      basis = fracMul(basis, [-xj, xi - xj]);
    }
    const term = fracMul([yi, 1n], basis);
    result = fracAdd(result, term);
  }
  return reduceFrac(result[0], result[1]);
}

// ---------- Main ----------
try {
  const infile = process.argv[2] || 'input.json';
  const raw = fs.readFileSync(infile, 'utf8');
  const j = JSON.parse(raw);

  if (!j.keys || typeof j.keys.k !== 'number') {
    console.error('input.json missing keys.k (k must be an integer).');
    process.exit(1);
  }
  const k = j.keys.k;

  const allPointKeys = Object.keys(j).filter(k2 => k2 !== 'keys');
  if (allPointKeys.length < k) {
    console.error(`Not enough points in JSON (have ${allPointKeys.length}, need ${k}).`);
    process.exit(1);
  }

  // sort safely using Number (since x keys are small)
  allPointKeys.sort((a, b) => Number(a) - Number(b));
  const selectedKeys = allPointKeys.slice(0, k);

  const points = selectedKeys.map(key => {
    const entry = j[key];
    if (!entry || !entry.base || entry.value === undefined) {
      throw new Error(`Bad entry for key ${key}`);
    }
    const base = Number(entry.base);
    if (!(base >= 2 && base <= 36)) throw new Error(`Unsupported base ${entry.base}`);
    const y = parseBaseToBigInt(entry.value, base);
    const x = BigInt(key);
    return { x, y };
  });

  const [num, den] = interpolateAtZero(points, k);
  if (den === 1n) {
    console.log(`Secret c = ${num.toString()}`);
  } else if (num % den === 0n) {
    console.log(`Secret c = ${(num / den).toString()}`);
  } else {
    console.log(`Secret c (fraction) = ${num.toString()} / ${den.toString()}`);
  }
} catch (err) {
  console.error('Error:', err.message || err);
  process.exit(1);
}
