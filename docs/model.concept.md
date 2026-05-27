---
ID: C_MDL
Title: Stroke Data Model
Status: active
Created: 2026-05-27
Updated: 2026-05-27
Depends on: []
Used by: [C_CTL, C_BOX]
---

# C_MDL — Stroke Data Model

## 01 — Purpose

Define the single immutable data structure that represents a completed or in-progress
drawn stroke. All stroke data flows through `PathWrapper`.

## 02 — Domain Model

A **stroke** is a sequence of pointer positions collected during one drag gesture,
plus the rendering attributes active at draw time (color, width, opacity).

The key design decision: **coordinates are stored in normalized [0..1] space**, not pixels.
This decouples the stored data from the canvas size, enabling the canvas to be resized
or the bitmap to be rendered at any resolution without re-processing stored paths.

## 03 — Mechanisms

**Normalization:** When a point is captured from a gesture, it is divided by the canvas
side length before storage. When rendered, all points are multiplied back by the target size.
`PathWrapper` itself is agnostic to this — it stores whatever floats it receives.

## 04 — What This IS and IS NOT

**IS:** A dumb data container. No methods, no validation, no I/O.  
**IS NOT:** A rendering primitive. The `box` layer converts `PathWrapper` to `Path` for drawing.

## 05 — Changelog

- 2026-05-27 — Initialized from existing codebase via onboard procedure.
