"""
상표 이미지 특징 벡터 추출기

- ResNetExtractor : ImageNet 사전학습 ResNet50 의 마지막 FC 층을 제거하고
                    2048 차원 임베딩을 사용한다. (PyTorch/torchvision 필요)
- LiteExtractor   : 딥러닝 라이브러리가 없는 저사양 PC 용 대체 추출기.
                    HOG(형태) + HSV 색상 히스토그램(색상) 을 결합한 1892 차원 벡터.

두 추출기 모두 L2 정규화된 벡터를 반환하므로 내적 = 코사인 유사도가 된다.
서로 다른 모델로 만든 벡터는 비교할 수 없으므로, 응답에 model 이름을 함께 넣고
Spring 서버는 같은 model 이름의 벡터끼리만 비교한다.
"""
from __future__ import annotations

import io
import logging
from typing import Protocol

import numpy as np
from PIL import Image

log = logging.getLogger(__name__)


class FeatureExtractor(Protocol):
    name: str
    dim: int

    def extract(self, image: Image.Image) -> np.ndarray: ...


def load_image(data: bytes) -> Image.Image:
    """바이트 → RGB 이미지. 투명 배경(PNG 로고)은 흰색 배경으로 합성한다."""
    img = Image.open(io.BytesIO(data))
    img.load()
    if img.mode in ("RGBA", "LA") or (img.mode == "P" and "transparency" in img.info):
        rgba = img.convert("RGBA")
        background = Image.new("RGBA", rgba.size, (255, 255, 255, 255))
        img = Image.alpha_composite(background, rgba)
    return img.convert("RGB")


def l2_normalize(v: np.ndarray) -> np.ndarray:
    norm = float(np.linalg.norm(v))
    return v if norm == 0 else v / norm


class LiteExtractor:
    """HOG + HSV 색상 히스토그램 (CPU, 추가 설치 불필요)"""

    name = "lite-hog-color-v1"
    size = 128

    def __init__(self) -> None:
        from skimage.feature import hog  # scikit-image

        self._hog = hog
        # 7x7 블록 * (2x2 셀) * 9 방향 = 1764, 색상 8x4x4 = 128 → 1892
        self.dim = 1764 + 128

    def extract(self, image: Image.Image) -> np.ndarray:
        img = image.resize((self.size, self.size), Image.Resampling.BILINEAR)
        gray = np.asarray(img.convert("L"), dtype=np.float32) / 255.0
        shape_feat = self._hog(
            gray,
            orientations=9,
            pixels_per_cell=(16, 16),
            cells_per_block=(2, 2),
            block_norm="L2-Hys",
            feature_vector=True,
        ).astype(np.float32)

        hsv = np.asarray(img.convert("HSV"), dtype=np.float32)
        hist, _ = np.histogramdd(
            hsv.reshape(-1, 3),
            bins=(8, 4, 4),
            range=((0, 256), (0, 256), (0, 256)),
        )
        color_feat = hist.flatten().astype(np.float32)

        # 형태 70%, 색상 30% 비중으로 결합
        vec = np.concatenate([l2_normalize(shape_feat) * 0.7, l2_normalize(color_feat) * 0.3])
        return l2_normalize(vec)


class ResNetExtractor:
    """ImageNet 사전학습 ResNet50 임베딩 (2048 차원)"""

    name = "resnet50-imagenet-v2"
    dim = 2048

    def __init__(self) -> None:
        import torch
        from torchvision import models, transforms

        self._torch = torch
        self.device = "cuda" if torch.cuda.is_available() else "cpu"
        weights = models.ResNet50_Weights.IMAGENET1K_V2
        model = models.resnet50(weights=weights)
        model.fc = torch.nn.Identity()  # 분류층 제거 → 특징 벡터 출력
        model.eval().to(self.device)
        self.model = model
        self.transform = transforms.Compose(
            [
                transforms.Resize(256),
                transforms.CenterCrop(224),
                transforms.ToTensor(),
                transforms.Normalize(mean=[0.485, 0.456, 0.406], std=[0.229, 0.224, 0.225]),
            ]
        )

    def extract(self, image: Image.Image) -> np.ndarray:
        tensor = self.transform(image).unsqueeze(0).to(self.device)
        with self._torch.no_grad():
            out = self.model(tensor)
        return l2_normalize(out.squeeze(0).cpu().numpy().astype(np.float32))


def build_extractor(backend: str = "auto") -> FeatureExtractor:
    """backend: resnet | lite | auto(resnet 시도 후 실패하면 lite)"""
    backend = (backend or "auto").lower()
    if backend in ("resnet", "auto"):
        try:
            extractor = ResNetExtractor()
            log.info("특징 추출기: %s (%s)", extractor.name, extractor.device)
            return extractor
        except Exception as exc:  # torch 미설치, 가중치 다운로드 실패 등
            if backend == "resnet":
                raise
            log.warning("ResNet50 을 불러오지 못해 lite 추출기로 전환합니다: %s", exc)
    extractor = LiteExtractor()
    log.info("특징 추출기: %s", extractor.name)
    return extractor


def cosine_similarity(a: np.ndarray, b: np.ndarray) -> float:
    na, nb = float(np.linalg.norm(a)), float(np.linalg.norm(b))
    if na == 0 or nb == 0:
        return 0.0
    return float(np.dot(a, b) / (na * nb))
