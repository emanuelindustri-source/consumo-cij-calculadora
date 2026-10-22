#!/usr/bin/env node
/* ============================================================
   Pruebas del motor de cálculo de la web (sin dependencias).

   No reimplementa las fórmulas: extrae el motor real de
   web/calculadora-offline.html y lo ejecuta sobre valores de
   referencia tomados de los modelos oficiales de Videojet.

   · CIJ Makeup Usage Model v14      → consumo de solvente
   · CIJ TCO Calculator v35          → gotas/litro, gotas/carácter, mantenimiento
   · CIJ Line Speed Calculator v9.7  → velocidad de línea

   Uso:  node tools/test_engine.js
   Sale con código 1 si alguna comprobación falla.
   ============================================================ */
"use strict";

const fs = require("fs");
const path = require("path");

const ROOT = path.dirname(__dirname);
const WEB = path.join(ROOT, "web", "calculadora-offline.html");

const html = fs.readFileSync(WEB, "utf8");

/* ---- extraer el motor puro del HTML (datos + funciones de cálculo) ---- */
const dataMatch = html.match(/^const DATA = .*?;$/m);
if (!dataMatch) throw new Error("no se encontró const DATA en la web");
const start = html.indexOf("const K_PUBLISHED");
const end = html.indexOf("RENDER DE RESULTADOS");
if (start < 0 || end < 0) throw new Error("no se encontró el bloque del motor");
const region = html.slice(start, html.lastIndexOf("/* =", end));

const factory = new Function(
  "clamp",
  dataMatch[0] + "\n" + region +
  "\nreturn { computeAll: computeAll, MODELS: MODELS, DATA: DATA," +
  " K_PUBLISHED: K_PUBLISHED, variantOf: variantOf, dropsPerChar: dropsPerChar," +
  " dropsLiterFor: dropsLiterFor, DEFAULT_PRINTER: DEFAULT_PRINTER };"
);
const E = factory((v, a, b) => Math.min(b, Math.max(a, v)));

/* ---- utilidades de prueba ---- */
let passed = 0;
const failures = [];
function check(name, cond, detail) {
  if (cond) {
    passed++;
  } else {
    failures.push(name + (detail ? "  → " + detail : ""));
  }
}
function near(name, actual, expected, tol) {
  const ok = isFinite(actual) && Math.abs(actual - expected) <= tol;
  check(name, ok, "esperado " + expected + " ± " + tol + " · obtenido " + actual);
}

/* ---- entradas por defecto (equipo real de planta) ---- */
function inp(over) {
  return Object.assign({
    printer: "VJ 1880+", nozzle: "60", ink: "V4201", inkBase: "MEK",
    tempModel: "published", inkCart: 750, muCart: 750,
    font: "5x7 Single", chars: 12, lines: 1, stroke: 1, width: 11,
    temp: 25, ppm: 60, hrsDay: 16, daysWeek: 5, daysMonth: 22, eff: 0.9,
    lineSpeed: 50, prodW: 120, gap: 30, shelfLife: 12, coreHours: 0, coreDays: 0,
    fHum: 1, fRecir: 1, fCond: 1, fNozzle: 1, fOther: 1
  }, over || {});
}

/* ============================================================
   1. Constantes de los modelos oficiales
   ============================================================ */
near("K_PUBLISHED = 2,923 / 6,475", E.K_PUBLISHED, 2.923 / 6.475, 1e-12);
near("gotas/car. matriz 5x7 = 13", E.DATA.dropsChar["5x7"], 13, 0);
near("gotas/car. matriz 7x9 = 19", E.DATA.dropsChar["7x9"], 19, 0);
near("gotas/car. matriz 10x16 = 61", E.DATA.dropsChar["10x16"], 61, 0);
near("gotas/litro 60 µm", E.dropsLiterFor("60", "VJ1000"), 1232240697, 1);
near("gotas/litro 70 µm", E.dropsLiterFor("70", "VJ1000"), 883500036, 1);
check("29 modelos cargados", E.MODELS.length === 29, "obtenidos " + E.MODELS.length);
check("equipo por defecto VJ 1880+", E.DEFAULT_PRINTER === "VJ 1880+");

/* ============================================================
   2. Solvente (CIJ Makeup Usage Model v14)
   ============================================================ */
// Documento oficial «CIJ Makeup Usage Model v14_VJ 1880+_V4201.pdf»:
// 286 h/L a 20 °C con boquilla de 60 µm y 222 h/L con 70 µm.
near("VJ 1880+ 60 µm → 286 h/L", E.variantOf(E.MODELS.find((m) => m.id === "VJ 1880+"), "60").hrsL, 286, 0);
near("VJ 1880+ 70 µm → 222 h/L", E.variantOf(E.MODELS.find((m) => m.id === "VJ 1880+"), "70").hrsL, 222, 0);

// Valor publicado en la documentación del proyecto: 5,21 ml/h (documento oficial: 5,2).
const r30 = E.computeAll(inp({ temp: 30 }));
near("VJ 1880+ / V4201 / 30 °C = 5,21 ml/h", r30.muPerHour, 5.21, 0.01);

const r20 = E.computeAll(inp({ temp: 20 }));
near("VJ 1880+ / V4201 / 20 °C = 3,50 ml/h", r20.muPerHour, 3.50, 0.01);

// La temperatura entra por exponencial: nunca baja al subir la temperatura.
let mono = true;
for (let t = 10; t < 40; t++) {
  const a = E.computeAll(inp({ temp: t })).muPerHour;
  const b = E.computeAll(inp({ temp: t + 1 })).muPerHour;
  if (!(b > a)) mono = false;
}
check("el consumo de solvente crece con la temperatura", mono);

// Los factores ambientales son multiplicadores: 2,0 dobla el consumo.
const rEnv = E.computeAll(inp({ temp: 30, fHum: 2 }));
near("factor ambiental 2,0 duplica el consumo", rEnv.muPerHour, r30.muPerHour * 2, 0.001);

// Base de solvente: MEK 1,00 · Ethanol 0,46 · Acetone 2,20.
near("base MEK → factor 1,00", E.DATA.inkBases.MEK.cons, 1, 0);
near("base Ethanol → factor 0,46", E.DATA.inkBases.Ethanol.cons, 0.46, 0);
near("base Acetone → factor 2,20", E.DATA.inkBases.Acetone.cons, 2.2, 0);

/* ============================================================
   3. Tinta (CIJ TCO Calculator v35)
   ============================================================ */
// gotas/mensaje = gotas/car. × car/línea × líneas × stroke
near("gotas/mensaje 5x7 × 12 × 1 × 1 = 156", r30.dropsMsg, 156, 0);
const rMulti = E.computeAll(inp({ font: "7x9 Single", chars: 20, lines: 2, stroke: 2 }));
near("gotas/mensaje 7x9 × 20 × 2 × 2 = 1520", rMulti.dropsMsg, 1520, 0);
const rQr = E.computeAll(inp({ font: "QR Code 21" }));
near("QR → 220 gotas/carácter", rQr.dpc, 220, 0);

// ml/impresión = (gotas/mensaje ÷ gotas/litro) × 1000
near("ml/impresión coherente con gotas/litro",
  r30.inkMlPerPrint, (r30.dropsMsg / r30.dpl) * 1000, 1e-12);
// el consumo de tinta no depende de la temperatura
near("la tinta no depende de la temperatura",
  E.computeAll(inp({ temp: 40 })).inkPerHour, r30.inkPerHour, 1e-12);

/* ============================================================
   4. Velocidad de línea (CIJ Line Speed Calculator v9.7)
   ============================================================ */
// v máx (m/min) = (caracteres/seg ÷ caracteres/línea) × ancho_mm × 0,06
near("v máx = (cps ÷ car/línea) × ancho × 0,06",
  r30.maxLineSpeed, (r30.cps / 12) * 11 * 0.06, 1e-9);
check("VJ 1880+ · 5x7 Single → 1600 car/seg", r30.cps === 1600, "obtenidos " + r30.cps);
near("velocidad nominal de la ficha (5x7 · VJ 1880+)", r30.nomSpeed, 243.84, 0.01);
near("v máx de la ficha (12 car. · 11 mm)", r30.maxLineSpeed, 88, 0.01);

// Sin datos de velocidad el motor no inventa cifras: devuelve null, no un 0.
let sinDatos = null;
for (let i = 0; i < E.MODELS.length && !sinDatos; i++) {
  const m = E.MODELS[i];
  const fonts = Object.keys(E.DATA.fonts);
  for (let j = 0; j < fonts.length; j++) {
    const rr = E.computeAll(inp({ printer: m.id, nozzle: m.nozzles[0], font: fonts[j] }));
    if (rr.cps === null) { sinDatos = { id: m.id, nozzle: m.nozzles[0], font: fonts[j] }; break; }
  }
}
check("hay combinaciones equipo/fuente sin datos de velocidad", !!sinDatos);
if (sinDatos) {
  const rr = E.computeAll(inp({ printer: sinDatos.id, nozzle: sinDatos.nozzle, font: sinDatos.font }));
  check("sin datos → v máx es null (no un 0 engañoso)", rr.maxLineSpeed === null,
    sinDatos.id + " · " + sinDatos.font + " → " + rr.maxLineSpeed);
}

/* ============================================================
   5. Escalado temporal y cartuchos
   ============================================================ */
near("ml/mes = ml/h × horas/día × días/mes",
  r30.muMonth, r30.muPerHour * 16 * 22, 1e-9);
near("ml/día = ml/h × horas/día", r30.muDay, r30.muPerHour * 16, 1e-9);
near("ml/año = ml/h × horas/día × días/semana × 52",
  r30.muYear, r30.muPerHour * 16 * 5 * 52, 1e-9);
near("horas por cartucho de solvente", r30.hrsPerMuCart, 750 / r30.muPerHour, 1e-9);
near("impresiones por cartucho de tinta", r30.printsPerInkCart, 750 / r30.inkMlPerPrint, 1e-6);
near("relación solvente:tinta", r30.ratio, r30.muMonth / r30.inkMonth, 1e-9);

/* ============================================================
   6. Mantenimiento (CIJ TCO v35 · Maintenance Setup)
   ============================================================ */
const m1620hr = (E.DATA.maintenance || {})["VJ 1620 HR"];
check("VJ 1620 HR → Core 14 000 h", m1620hr && m1620hr.items[0].hours === 14000);
const m1650hr = (E.DATA.maintenance || {})["VJ 1650 HR"];
check("VJ 1650 HR → Core 14 000 h", m1650hr && m1650hr.items[0].hours === 14000);
const m1650uhs = (E.DATA.maintenance || {})["VJ 1650 UHS"];
check("VJ 1650 UHS → Core 14 000 h", m1650uhs && m1650uhs.items[0].hours === 14000);

/* ============================================================
   7. Tintas sin datos oficiales (V4238 / V4251)
   ============================================================ */
check("V4238 se marca sin datos oficiales",
  E.computeAll(inp({ ink: "V4238" })).officialData === false);
check("V4251 se marca sin datos oficiales",
  E.computeAll(inp({ ink: "V4251" })).officialData === false);
check("V4201 sí tiene datos oficiales",
  E.computeAll(inp({ ink: "V4201" })).officialData === true);

/* ============================================================
   8. Tamaños de cartucho reales (TCO v35)
   ============================================================ */
const carts = E.DATA.inkContainers || {};
check("V4225 → cartucho de 700 ml", (carts.V4225 && carts.V4225.inkSizes.indexOf(700) >= 0));
check("V480 → cartucho de 600 ml", (carts.V480 && carts.V480.inkSizes.indexOf(600) >= 0));
check("V401 → 750 / 5000 ml", (carts.V401 && carts.V401.inkSizes.indexOf(750) >= 0 &&
  carts.V401.inkSizes.indexOf(5000) >= 0));

/* ============================================================
   Resumen
   ============================================================ */
console.log("Pruebas del motor de la web: " + passed + " correctas, " + failures.length + " fallos.");
if (failures.length) {
  failures.forEach((f) => console.log("  FALLO  " + f));
  process.exit(1);
}
console.log("Todo correcto.");
