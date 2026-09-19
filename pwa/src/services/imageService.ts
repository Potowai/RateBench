import imageCompression from 'browser-image-compression';
import { supabase } from '../lib/supabase';

export interface CompressionOptions {
  maxSizeMB?: number;
  maxWidthOrHeight?: number;
  useWebWorker?: boolean;
}

/**
 * Compresse une image côté client pour qu'elle pèse moins de 300 Ko
 * et soit optimisée pour un affichage fluide sur mobile.
 */
export async function compressImage(
  imageFile: File,
  customOptions?: CompressionOptions
): Promise<File> {
  const options = {
    maxSizeMB: customOptions?.maxSizeMB ?? 0.3, // Moins de 300 Ko
    maxWidthOrHeight: customOptions?.maxWidthOrHeight ?? 1400,
    useWebWorker: customOptions?.useWebWorker ?? true,
    fileType: 'image/webp'
  };

  try {
    const compressedBlob = await imageCompression(imageFile, options);
    // Conserve le nom original avec extension .webp
    const fileName = imageFile.name.replace(/\.[^/.]+$/, "") + ".webp";
    return new File([compressedBlob], fileName, { type: 'image/webp' });
  } catch (error) {
    console.warn("Échec de la compression, utilisation de l'image originale:", error);
    return imageFile;
  }
}

/**
 * Téléverse la photo compressée vers le bucket public 'bench-photos' de Supabase Storage.
 * Retourne l'URL publique de l'image.
 */
export async function uploadBenchPhoto(
  file: File,
  userId?: string
): Promise<string> {
  const compressed = await compressImage(file);
  const fileExt = 'webp';
  const timestamp = Date.now();
  const randomStr = Math.random().toString(36).substring(2, 8);
  const userPrefix = userId ? `${userId}/` : 'public/';
  const filePath = `${userPrefix}${timestamp}_${randomStr}.${fileExt}`;

  const { error: uploadError } = await supabase.storage
    .from('bench-photos')
    .upload(filePath, compressed, {
      contentType: 'image/webp',
      cacheControl: '31536000', // 1 an de cache navigateur
      upsert: false
    });

  if (uploadError) {
    throw new Error(`Erreur lors du téléversement de la photo: ${uploadError.message}`);
  }

  const { data: publicUrlData } = supabase.storage
    .from('bench-photos')
    .getPublicUrl(filePath);

  return publicUrlData.publicUrl;
}
