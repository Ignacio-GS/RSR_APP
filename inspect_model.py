#!/usr/bin/env python3
"""Script para inspeccionar modelo TFLite y ver sus dimensiones"""

import tensorflow as tf
import sys

def inspect_tflite_model(model_path):
    """Inspecciona un modelo TFLite y muestra sus dimensiones"""

    # Cargar el intérprete
    interpreter = tf.lite.Interpreter(model_path=model_path)
    interpreter.allocate_tensors()

    # Obtener detalles de entrada
    input_details = interpreter.get_input_details()
    output_details = interpreter.get_output_details()

    print("="*60)
    print("INFORMACIÓN DEL MODELO TFLITE")
    print("="*60)

    print("\n📥 ENTRADA:")
    for i, detail in enumerate(input_details):
        print(f"  [{i}] Nombre: {detail['name']}")
        print(f"      Shape: {detail['shape']}")
        print(f"      Dtype: {detail['dtype']}")

    print("\n📤 SALIDA:")
    for i, detail in enumerate(output_details):
        print(f"  [{i}] Nombre: {detail['name']}")
        print(f"      Shape: {detail['shape']}")
        print(f"      Dtype: {detail['dtype']}")

    print("\n" + "="*60)

    # Determinar parámetros para Kotlin
    if len(output_details) > 0:
        output_shape = output_details[0]['shape']
        print("\n🔧 PARÁMETROS PARA YOLODETECTORTFLITE.KT:")
        print(f"   INPUT_SIZE = {input_details[0]['shape'][1]}")
        if len(output_shape) == 3:  # [1, features, detections]
            print(f"   MAX_DETECTION = {output_shape[2]}")
            print(f"   OUTPUT_FEATURES = {output_shape[1]}")
            print(f"   OUTPUT_CLASSES = {output_shape[1] - 4}  // features - 4 (bbox coords)")
        print("="*60)

if __name__ == "__main__":
    if len(sys.argv) < 2:
        print("Uso: python inspect_model.py <ruta_al_modelo.tflite>")
        sys.exit(1)

    model_path = sys.argv[1]
    inspect_tflite_model(model_path)
