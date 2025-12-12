#!/bin/bash
# Script para configurar el entorno de entrenamiento YOLOv8

echo "======================================"
echo "Configurando entorno YOLOv8 Training"
echo "======================================"

# Crear entorno virtual
echo "📦 Creando entorno virtual..."
python3 -m venv yolo_env

# Activar entorno
source yolo_env/bin/activate

# Actualizar pip
echo "📦 Actualizando pip..."
pip install --upgrade pip

# Instalar dependencias
echo "📦 Instalando dependencias..."
pip install ultralytics
pip install torch torchvision torchaudio
pip install opencv-python
pip install pillow
pip install pyyaml
pip install tensorboard

echo ""
echo "✅ Entorno configurado exitosamente"
echo ""
echo "Para activar el entorno:"
echo "  source yolo_training/yolo_env/bin/activate"
echo ""
