package dev.sdras.productservice.resources;

import dev.sdras.productservice.domain.Product;
import dev.sdras.productservice.services.InstanceInformationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/products")
public class ProductResource {
    final InstanceInformationService instanceInformationService;

    public ProductResource(InstanceInformationService instanceInformationService) {
        this.instanceInformationService = instanceInformationService;
    }

    @GetMapping
    public Product getProduct() {
        int id = (int) (Math.random() * 100);
        return Product.builder()
                .id(id)
                .name("Product " + id)
                .description("Product description id:" + id)
                .build();
    }
}
