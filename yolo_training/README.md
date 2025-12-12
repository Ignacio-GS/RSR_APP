# Entrenamiento YOLOv8e para Productos PepsiCo

Guía completa para entrenar un modelo YOLOv8e personalizado para detección de productos PepsiCo.

## 📋 Requisitos

- Python 3.8+
- GPU NVIDIA con CUDA (recomendado) o CPU
- Al menos 8GB RAM
- Dataset de imágenes etiquetadas

## 🗂️ Estructura del Dataset

Tu dataset debe seguir el formato YOLO:

```
dataset/
├── images/
│   ├── train/
│   │   ├── img1.jpg
│   │   ├── img2.jpg
│   │   └── ...
│   └── val/
│       ├── img1.jpg
│       └── ...
└── labels/
    ├── train/
    │   ├── img1.txt
    │   ├── img2.txt
    │   └── ...
    └── val/
        ├── img1.txt
        └── ...
```

### Formato de las etiquetas (.txt)

Cada archivo de etiqueta contiene una línea por objeto:
```
<class_id> <x_center> <y_center> <width> <height>
```

Donde:
- `class_id`: ID de la clase (0-6 para tus 7 productos)
- `x_center, y_center, width, height`: Coordenadas normalizadas [0.0-1.0]

Ejemplo (`img1.txt`):
```
4 0.512 0.345 0.234 0.456  # Pepsi en el centro
0 0.123 0.678 0.156 0.234  # 7up a la izquierda
```

## 🎯 Clases de Productos

0. **7up**
1. **Cheetos**
2. **Manzanita Sol**
3. **Mirinda**
4. **Pepsi**
5. **Pepsi Black**
6. **Squirt**

## 🚀 Pasos de Entrenamiento

### 1. Configurar el Entorno

```bash
cd yolo_training
bash setup.sh
source yolo_env/bin/activate
```

### 2. Preparar tu Dataset

Organiza tus imágenes y etiquetas en la estructura mostrada arriba.

#### Opción A: Dataset ya etiquetado
Copia tu dataset a `yolo_training/dataset/`

#### Opción B: Etiquetar desde cero
Usa herramientas como:
- [Roboflow](https://roboflow.com/) (recomendado, web-based)
- [LabelImg](https://github.com/heartexlabs/labelImg) (desktop)
- [CVAT](https://www.cvat.ai/) (web-based)

**Recomendaciones:**
- Mínimo **200 imágenes por clase** para entrenamiento
- Al menos **50 imágenes por clase** para validación
- Captura desde diferentes ángulos y condiciones de iluminación
- Incluye productos en diferentes contextos (estantes, cajas, etc.)

### 3. Verificar el Dataset

```bash
# Listar imágenes de entrenamiento
ls dataset/images/train/ | wc -l

# Listar etiquetas de entrenamiento
ls dataset/labels/train/ | wc -l

# Deben ser iguales
```

### 4. Entrenar el Modelo

```bash
python train_yolov8e.py
```

**Parámetros importantes en el script:**
- `epochs`: 100 (ajusta según tus resultados)
- `batch`: 16 (reduce si te quedas sin memoria GPU)
- `imgsz`: 640 (tamaño de entrada del modelo)
- `patience`: 20 (early stopping si no mejora)

### 5. Monitorear el Entrenamiento

Durante el entrenamiento verás:
```
Epoch    GPU_mem   box_loss   cls_loss   dfl_loss  Instances       Size
  1/100      5.2G      1.234      0.876      1.123         45        640
```

También puedes usar TensorBoard:
```bash
tensorboard --logdir runs/detect/pepsico_yolov8e
```

### 6. Validar Resultados

Después del entrenamiento, revisa:
- `runs/detect/pepsico_yolov8e/weights/best.pt` - Mejor modelo
- `runs/detect/pepsico_yolov8e/results.png` - Gráficas de métricas
- `runs/detect/pepsico_yolov8e/confusion_matrix.png` - Matriz de confusión

### 7. Exportar a TFLite

El script ya exporta automáticamente, pero también puedes hacerlo manualmente:

```bash
python export_to_tflite.py
```

### 8. Integrar en la App

```bash
# Inspeccionar modelo
python ../inspect_model.py runs/detect/pepsico_yolov8e/weights/best_float32.tflite

# Copiar a la app
cp runs/detect/pepsico_yolov8e/weights/best_float32.tflite \
   ../app/src/main/assets/best_float32.tflite
```

## 📊 Métricas Objetivo

Para un buen modelo de detección de productos:
- **mAP50**: > 0.85 (85%)
- **mAP50-95**: > 0.65 (65%)
- **Inference time**: < 200ms en dispositivo móvil

## 🔧 Troubleshooting

### Problema: Out of Memory (OOM)

```python
# En train_yolov8e.py, reduce el batch size:
'batch': 8,  # o 4 para GPUs pequeñas
```

### Problema: Modelo no converge

- Verifica que las etiquetas estén correctas
- Aumenta el número de imágenes de entrenamiento
- Ajusta learning rate: `'lr0': 0.0005`

### Problema: Overfitting

- Aumenta data augmentation
- Reduce epochs
- Añade más imágenes de validación

### Problema: Clase desbalanceada

Si una clase tiene muchas más imágenes:
- Usa data augmentation en clases minoritarias
- Ajusta class weights en el entrenamiento

## 📚 Recursos Adicionales

- [Documentación Ultralytics YOLOv8](https://docs.ultralytics.com/)
- [Tutorial de dataset YOLO](https://docs.ultralytics.com/datasets/detect/)
- [Tips de entrenamiento](https://docs.ultralytics.com/guides/model-training-tips/)

## 🆘 Soporte

Si necesitas ayuda:
1. Revisa los logs de entrenamiento
2. Verifica el formato del dataset
3. Consulta la documentación de Ultralytics
