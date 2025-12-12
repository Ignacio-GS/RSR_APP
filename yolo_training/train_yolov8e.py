#!/usr/bin/env python3
"""
Script para entrenar modelo YOLOv8e para detección de productos PepsiCo
"""

from ultralytics import YOLO
import torch
import os

def main():
    print("="*60)
    print("ENTRENAMIENTO YOLOV8e - PRODUCTOS PEPSICO")
    print("="*60)

    # Verificar CUDA
    device = 'cuda' if torch.cuda.is_available() else 'cpu'
    print(f"\n🖥️  Dispositivo: {device}")
    if device == 'cuda':
        print(f"    GPU: {torch.cuda.get_device_name(0)}")

    # Cargar modelo pre-entrenado YOLOv8e
    print("\n📥 Cargando modelo YOLOv8e pre-entrenado...")
    model = YOLO('yolov8e.pt')  # Efficient version

    # Configuración de entrenamiento
    print("\n⚙️  Configuración de entrenamiento:")
    config = {
        'data': 'dataset.yaml',
        'epochs': 100,
        'imgsz': 640,
        'batch': 16,  # Ajustar según tu GPU
        'device': device,
        'workers': 8,
        'patience': 20,  # Early stopping
        'save': True,
        'save_period': 10,  # Guardar cada 10 epochs
        'project': 'runs/detect',
        'name': 'pepsico_yolov8e',
        'exist_ok': True,
        'pretrained': True,
        'optimizer': 'Adam',
        'lr0': 0.001,
        'lrf': 0.01,
        'momentum': 0.937,
        'weight_decay': 0.0005,
        'warmup_epochs': 3,
        'warmup_momentum': 0.8,
        'box': 7.5,
        'cls': 0.5,
        'dfl': 1.5,
        'augment': True,
        'hsv_h': 0.015,
        'hsv_s': 0.7,
        'hsv_v': 0.4,
        'degrees': 0.0,
        'translate': 0.1,
        'scale': 0.5,
        'shear': 0.0,
        'perspective': 0.0,
        'flipud': 0.0,
        'fliplr': 0.5,
        'mosaic': 1.0,
        'mixup': 0.0,
    }

    for key, value in config.items():
        print(f"    {key}: {value}")

    # Entrenar
    print("\n🚀 Iniciando entrenamiento...\n")
    results = model.train(**config)

    print("\n✅ Entrenamiento completado!")
    print(f"📁 Modelo guardado en: runs/detect/pepsico_yolov8e/weights/best.pt")

    # Validar modelo
    print("\n📊 Validando modelo...")
    metrics = model.val()

    print("\n📈 Métricas finales:")
    print(f"    mAP50: {metrics.box.map50:.4f}")
    print(f"    mAP50-95: {metrics.box.map:.4f}")

    # Exportar a TFLite
    print("\n📦 Exportando a TFLite...")
    export_path = model.export(
        format='tflite',
        imgsz=640,
        int8=False,  # Float32
        optimize=True
    )

    print(f"✅ Modelo TFLite exportado: {export_path}")
    print("\n" + "="*60)
    print("PRÓXIMOS PASOS:")
    print("="*60)
    print(f"1. Inspecciona el modelo TFLite:")
    print(f"   python ../inspect_model.py {export_path}")
    print(f"\n2. Copia el modelo a la app:")
    print(f"   cp {export_path} ../app/src/main/assets/best_float32.tflite")
    print("="*60)

if __name__ == "__main__":
    main()
