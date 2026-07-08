package com.example.product_service.service;

import com.example.product_service.dto.ProductDTO;
import com.example.product_service.entity.Product;
import com.example.product_service.exception.ProductNotFoundException;
import com.example.product_service.mapper.ProductMapper;
import com.example.product_service.repository.ProductRepository;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {

    private static final Logger logger = LoggerFactory.getLogger(ProductService.class);

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    // Get All Products
    public List<ProductDTO> getAllProducts() {

        logger.info("Fetching all products");

        return productRepository.findAll()
                .stream()
                .map(ProductMapper::toDTO)
                .collect(Collectors.toList());
    }

    // Get Product By ID
    public ProductDTO getProductById(Long id) {

        logger.info("Fetching product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product with ID " + id + " not found"));

        logger.info("Product found successfully.");

        return ProductMapper.toDTO(product);
    }

    // Create Product
    public ProductDTO createProduct(ProductDTO dto) {

        logger.info("Creating product: {}", dto.getName());

        Product product = ProductMapper.toEntity(dto);

        Product savedProduct = productRepository.save(product);

        logger.info("Product created successfully with ID: {}", savedProduct.getId());

        return ProductMapper.toDTO(savedProduct);
    }

    // Update Product
    public ProductDTO updateProduct(Long id, ProductDTO dto) {

        logger.info("Updating product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product with ID " + id + " not found"));

        product.setName(dto.getName());
        product.setDescription(dto.getDescription());
        product.setPrice(dto.getPrice());
        product.setQuantity(dto.getQuantity());

        Product updatedProduct = productRepository.save(product);

        logger.info("Product updated successfully.");

        return ProductMapper.toDTO(updatedProduct);
    }

    // Delete Product
    public void deleteProduct(Long id) {

        logger.info("Deleting product with ID: {}", id);

        Product product = productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException("Product with ID " + id + " not found"));

        productRepository.delete(product);

        logger.info("Product deleted successfully.");
    }
}