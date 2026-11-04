#!/usr/bin/env node
/* ============================================================
   Validación de las 800 fichas de solvente del
   CIJ Makeup Usage Model v14 (12.800 valores, 10–40 °C).

   El fixture tools/fixtures/solvent_cards.json se generó a partir
   de los PDF oficiales por equipo/tinta. El motor real se extrae de
   web/calculadora-offline.html, igual que en test_engine.js.

   Uso:  node tools/test_solvent.js
   ============================================================ */
"use strict";

const fs = require("fs");
const path = require("path");

const ROOT = path.dirname(__dirname);
const WEB = path.join(ROOT, "web", "calculadora-offline.html");
const FIXTURE = path.join(ROOT, "tools", "fixtures", "solvent_cards.json");
const html = fs.readFileSync(WEB, "utf8");
const cards = JSON.parse(fs.readFileSync(FIXTURE, "utf8"));

const dataMatch = html.match(/^const DATA = .*?;$/m);
const start = html.indexOf("const K_PUBLISHED");
const end = html.indexOf("RENDER DE RESULTADOS");
if (!dataMatch || start < 0 || end < 0) throw new Error("no se encontró el motor de la web");
const region = html.slice(start, html.lastIndexOf("/* =", end));
const factory = new Function(
  "clamp",
  dataMatch[0] + "\n" + region +
  "\nreturn { MODELS: MODELS, DATA: DATA, K_PUBLISHED: K_PUBLISHED, variantOf: variantOf };"
);
const E = factory((v, a, b) => Math.min(b, Math.max(a, v)));

function baseName(s) { return String(s).replace(/\s*(60|70|50|40)\s*µm.*$/i, "").trim(); }
function nozzleOf(s) {
  const m = String(s).match(/\s*(60|70|50|40)\s*µm/i);
  return m ? m[1] : null;
}

let checked = 0;
const failures = [];
const models = new Map();
for (const m of E.MODELS) models.set(m.id, m);

for (const card of cards) {
  const id = baseName(card.model);
  const nz = nozzleOf(card.model);
  const model = models.get(id);
  if (!model) {
    failures.push("modelo no encontrado: " + card.model);
    continue;
  }
  const variant = (nz && model.variants[nz]) ? model.variants[nz] : model.defaultVariant;
  const base = E.DATA.inkBases[card.base];
  if (!base) {
    failures.push("base no encontrada: " + card.base);
    continue;
  }
  for (let i = 0; i < card.values.length; i++) {
    const temp = 10 + i * 2;
    const got = (1000 / variant.hrsL) * base.cons * E.K_PUBLISHED * Math.exp(base.k[0] * temp);
    const expected = card.values[i];
    checked++;
    if (!(Math.abs(got - expected) <= 0.06)) {
      failures.push(card.model + " · " + card.ink + " · " + temp + " °C: esperado " +
        expected + ", obtenido " + got.toFixed(3));
    }
  }
}

console.log("Fichas de solvente: " + cards.length + " · valores: " + checked);
if (failures.length) {
  for (const f of failures.slice(0, 20)) console.log("  FALLO " + f);
  if (failures.length > 20) console.log("  … y " + (failures.length - 20) + " más");
  console.log("VALIDACIÓN DE SOLVENTE FALLIDA (" + failures.length + ")");
  process.exit(1);
}
console.log("Validación de solvente correcta: " + checked + "/" + checked + " valores");
