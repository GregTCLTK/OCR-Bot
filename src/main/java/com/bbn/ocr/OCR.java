package com.bbn.ocr;

import com.google.cloud.vision.v1.*;
import com.google.cloud.vision.v1.Image;
import com.google.protobuf.ByteString;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class OCR {

    public static String handle(String url, MessageReceivedEvent event) {
        try {
            BufferedImage first = ImageIO.read(new URL(url));
            BufferedImage image = cropImage(first, first.getWidth()-350, first.getHeight()-550, 350, 450);
            ImageIO.write(image, "png", new File("./lastpicture.png"));
            event.getChannel().sendFile(new File("./lastpicture.png")).queue();
            return googleHandle();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String googleHandle() throws IOException {
        List<AnnotateImageRequest> requests = new ArrayList<>();

        ByteString imgBytes = ByteString.readFrom(new FileInputStream(new File("./lastpicture.png")));

        Image img = Image.newBuilder().setContent(imgBytes).build();
        Feature feat = Feature.newBuilder().setType(Feature.Type.TEXT_DETECTION).build();
        AnnotateImageRequest request =
                AnnotateImageRequest.newBuilder().addFeatures(feat).setImage(img).build();
        requests.add(request);

        try (ImageAnnotatorClient client = ImageAnnotatorClient.create()) {
            BatchAnnotateImagesResponse response = client.batchAnnotateImages(requests);
            List<AnnotateImageResponse> responses = response.getResponsesList();
            for (AnnotateImageResponse res : responses) {
                if (res.hasError()) {
                    return "Error: "+ res.getError().getMessage();
                }

                for (EntityAnnotation annotation : res.getTextAnnotationsList()) {
                    return annotation.getDescription();
                    // sb.append("Position: " + annotation.getBoundingPoly()+"\n");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static BufferedImage cropImage(BufferedImage bufferedImage, int x, int y, int width, int height){
        return bufferedImage.getSubimage(x, y, width, height);
    }
}
