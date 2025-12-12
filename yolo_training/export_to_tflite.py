#!/usr/bin/env python3
"""
Script para exportar modelo YOLOv8 entrenado a TensorFlow Lite
"""

from ultralytics import YOLO
import sys

def export_model(model_path='runs/detect/pepsico_yolov8e/weights/best.pt', output_name='best_float32'):
    """
    Exporta un modelo YOLOv8 .pt a TFLite

    Args:
        model_path: Ruta al modelo .pt entrenado
        output_name: Nombre del archivo de salida (sin extensión)
    """
    print("="*60)
    print("EXPORTANDO MODELO A TFLITE")
    print("="*60)

    # Cargar modelo entrenado
    print(f"\n📥 Cargando modelo: {model_path}")
    model = YOLO(model_path)

    # Exportar a TFLite
    print("\n🔄 Exportando a TensorFlow Lite...")
    print("    - Formato: TFLite (float32)")
    print("    - Tamaño de entrada: 640x640")
    print("    - Optimización: Activada")

    export_path = model.export(
        format='tflite',
        imgsz=640,
        int8=False,  # Usar float32 para mejor precisión
        half=False,  # No usar half precision
        optimize=True,
        simplify=True
    )

    print(f"\n✅ Exportación completada: {export_path}")

    # Mostrar información del archivo
    import os
    file_size = os.path.getsize(export_path) / (1024 * 1024)  # MB
    print(f"📊 Tamaño del archivo: {file_size:.2f} MB")

    print("\n" + "="*60)
    print("PRÓXIMOS PASOS:")
    print("="*60)
    print(f"1. Inspeccionar el modelo:")
    print(f"   python ../inspect_model.py {export_path}")
    print(f"\n2. Copiar a la app Android:")
    print(f"   cp {export_path} ../app/src/main/assets/best_float32.tflite")
    print("="*60)

    return export_path

if __name__ == "__main__":
    if len(sys.argv) > 1:
        model_path = sys.argv[1]
        export_model(model_path)
    else:
        export_model()
